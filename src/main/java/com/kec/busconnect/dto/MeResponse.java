package com.kec.busconnect.dto;

import com.kec.busconnect.model.Bus;
import com.kec.busconnect.model.Route;
import com.kec.busconnect.model.Student;
import com.kec.busconnect.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {
    private User user;
    private Student student;
    private Bus assignedBus;
    private Route assignedRoute;
}
