package com.kec.busconnect.service;

import com.kec.busconnect.enums.BusStatus;
import com.kec.busconnect.exception.BadRequestException;
import com.kec.busconnect.exception.ResourceNotFoundException;
import com.kec.busconnect.model.Bus;
import com.kec.busconnect.repository.BusRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusService {

    private final BusRepository busRepository;

    public BusService(BusRepository busRepository) {
        this.busRepository = busRepository;
    }

    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    public List<Bus> getActiveBuses() {
        return busRepository.findByIsActive(true);
    }

    public Bus getBusById(String id) {
        return busRepository.findById(id)
                .or(() -> busRepository.findByBusNumber(id))
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with ID or number: " + id));
    }

    public Bus getBusByBusNumber(String busNumber) {
        return busRepository.findByBusNumber(busNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with number: " + busNumber));
    }

    public Bus createBus(Bus bus) {
        if (busRepository.existsByBusNumber(bus.getBusNumber())) {
            throw new BadRequestException("Bus number already exists");
        }
        return busRepository.save(bus);
    }

    public Bus updateBus(String id, Bus updatedBus) {
        Bus bus = getBusById(id);
        
        if (updatedBus.getBusNumber() != null && !updatedBus.getBusNumber().equals(bus.getBusNumber())) {
            if (busRepository.existsByBusNumber(updatedBus.getBusNumber())) {
                throw new BadRequestException("Bus number already exists");
            }
            bus.setBusNumber(updatedBus.getBusNumber());
        }
        
        if (updatedBus.getRegistrationNumber() != null) {
            bus.setRegistrationNumber(updatedBus.getRegistrationNumber());
        }
        if (updatedBus.getRouteId() != null) {
            bus.setRouteId(updatedBus.getRouteId());
        }
        if (updatedBus.getTrackerId() != null) {
            bus.setTrackerId(updatedBus.getTrackerId());
        }
        if (updatedBus.getStatus() != null) {
            bus.setStatus(updatedBus.getStatus());
        }
        bus.setActive(updatedBus.isActive());

        return busRepository.save(bus);
    }

    public void deleteBus(String id) {
        Bus bus = getBusById(id);
        busRepository.delete(bus);
    }
}
