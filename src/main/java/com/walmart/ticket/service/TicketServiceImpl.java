package com.walmart.ticket.service;

import com.walmart.ticket.domain.*;
import com.walmart.ticket.exception.SeatHoldNotFoundException;
import com.walmart.ticket.repository.CustomerRepository;
import com.walmart.ticket.repository.SeatHoldRepository;
import com.walmart.ticket.repository.SeatRepository;
import com.walmart.ticket.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
public class TicketServiceImpl implements TicketService {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private CustomerRepository customerRespository;

    @Autowired
    private SeatRepository seatRepository;

    @Override
    public int numSeatsAvailable() {
        return getVenueByID()
                .getAvailableSeats();
    }

    @Override
    public SeatHold findAndHoldSeats(int numSeats, String customerEmail) {
        if (numSeats <= 0) {
            throw new IllegalArgumentException("Invalid number of seats to hold: " + numSeats + "; cannot be zero or less than zero");
        }

        Venue venue = getVenueByID();

        int availableSeats = venue.getAvailableSeats();

        if ( availableSeats <= 0 ) {
            throw new IllegalArgumentException("No seats are available in the venue");
        }
        else if (availableSeats < numSeats) {
            throw new IllegalArgumentException("Not enough seats are available in venue for holding seats: " + numSeats);
        }

        Customer customer = getOrCreateCustomerForCustomerEmail(customerEmail);

        SeatHold seatHold = SeatHold.newInstance(customer, venue, numSeats);

        seatHoldRepository.save(seatHold);

        return seatHold;
    }

    private Customer getOrCreateCustomerForCustomerEmail(String customerEmail) {
        Customer customer = customerRespository.findByCustomerEmailIgnoreCase(customerEmail)
                .orElse(new Customer(customerEmail));

        customerRespository.save(customer);
        return customer;
    }

    private Venue getVenueByID() {
        return venueRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("Venue is not found"));
    }

    @Override
    public String reserveSeats(String seatHoldId, String customerEmail) {

        SeatHold seatHold = seatHoldRepository.findById(Long.valueOf(seatHoldId))
                .orElseThrow(() -> new SeatHoldNotFoundException(Long.valueOf(seatHoldId), "seatHoldId is not found"));

        seatHold.validateCustomerEmail(customerEmail);
        seatHold.validateBooking();
        seatHold.addBooking();
        seatHold.makeReservedSeats();

        seatHoldRepository.save(seatHold);

        return seatHold.getBookingCode()
                .orElseThrow(() -> new IllegalStateException("Unable to find booking code"));
    }
}
