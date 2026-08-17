package com.kec.busconnect.config;

import com.kec.busconnect.enums.*;
import com.kec.busconnect.model.*;
import com.kec.busconnect.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final BusLocationRepository busLocationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           StudentRepository studentRepository,
                           BusRepository busRepository,
                           RouteRepository routeRepository,
                           BusLocationRepository busLocationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
        this.busLocationRepository = busLocationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            // Clean up any old admin record that was stored with bare "admin" email
            try {
                userRepository.findByEmail("admin").ifPresent(userRepository::delete);
            } catch (Exception e) {
                System.out.println("Notice: admin cleanup check: " + e.getMessage());
            }

            // Always ensure the default admin user exists with the correct credentials
            User adminUser = null;
            try {
                adminUser = userRepository.findByEmail("admin@kec.ac.in").orElse(new User());
                adminUser.setEmail("admin@kec.ac.in");
                adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
                adminUser.setRole(Role.ADMIN);
                adminUser.setActive(true);
                userRepository.save(adminUser);
                System.out.println("Default admin user verified (admin@kec.ac.in).");
            } catch (Exception e) {
                System.out.println("Notice: admin initialization: " + e.getMessage());
            }

            // Always ensure default driver user exists
            User driverUser = null;
            try {
                driverUser = userRepository.findByEmail("driver@kec.ac.in").orElse(new User());
                driverUser.setEmail("driver@kec.ac.in");
                driverUser.setPasswordHash(passwordEncoder.encode("password"));
                driverUser.setRole(Role.DRIVER);
                driverUser.setActive(true);
                userRepository.save(driverUser);
                System.out.println("Default driver user verified (driver@kec.ac.in).");
            } catch (Exception e) {
                System.out.println("Notice: driver initialization: " + e.getMessage());
            }

            // Always ensure backup driver user exists
            try {
                User trackerUser = userRepository.findByEmail("tracker@kec.ac.in").orElse(new User());
                trackerUser.setEmail("tracker@kec.ac.in");
                trackerUser.setPasswordHash(passwordEncoder.encode("password"));
                trackerUser.setRole(Role.DRIVER);
                trackerUser.setActive(true);
                userRepository.save(trackerUser);
                System.out.println("Default tracker/driver verified (tracker@kec.ac.in).");
            } catch (Exception e) {
                System.out.println("Notice: tracker initialization: " + e.getMessage());
            }

            // Remove legacy buses and routes to ensure only Bus KEC-07 and Attikuppam route remain active
            try {
                busRepository.findAll().forEach(bus -> {
                    if (!"KEC-07".equalsIgnoreCase(bus.getBusNumber())) {
                        busRepository.delete(bus);
                    }
                });

                routeRepository.findAll().forEach(r -> {
                    if (!"Attikuppam → KEC (via MDR87)".equalsIgnoreCase(r.getName())) {
                        routeRepository.delete(r);
                    }
                });
            } catch (Exception e) {
                System.out.println("Notice: legacy routes cleanup: " + e.getMessage());
            }

            Route.PointName startMDR = new Route.PointName("Attikuppam (Origin)");
            Route.PointName destMDR = new Route.PointName("Kuppam Engineering College (KEC - Terminus)");
            Route routeMDR = routeRepository.findByName("Attikuppam → KEC (via MDR87)").orElse(new Route());
            routeMDR.setName("Attikuppam → KEC (via MDR87)");
            routeMDR.setStartPoint(startMDR);
            routeMDR.setDestination(destMDR);
            routeMDR.setStops(Arrays.asList(
                    new Route.Stop("Attikuppam (Origin)", 1, new GeoPoint("Point", Arrays.asList(78.479812, 12.884713))),
                    new Route.Stop("Manendram Village Stop", 2, new GeoPoint("Point", Arrays.asList(78.481943, 12.878439))),
                    new Route.Stop("Balaobanapalle Northern Junction", 3, new GeoPoint("Point", Arrays.asList(78.472352, 12.835211))),
                    new Route.Stop("Singasamudram Center", 4, new GeoPoint("Point", Arrays.asList(78.503606, 12.833760))),
                    new Route.Stop("Kenchanaballa (Loop Terminus)", 5, new GeoPoint("Point", Arrays.asList(78.482298, 12.828577))),
                    new Route.Stop("Singasamudram (Return Pass-through)", 6, new GeoPoint("Point", Arrays.asList(78.503606, 12.833760))),
                    new Route.Stop("Balaobanapalle Junction (Return Axis)", 7, new GeoPoint("Point", Arrays.asList(78.472352, 12.835211))),
                    new Route.Stop("Vijayapuram (Vijalapuram)", 8, new GeoPoint("Point", Arrays.asList(78.453880, 12.841468))),
                    new Route.Stop("Aniganur (Sachivalayam Stop)", 9, new GeoPoint("Point", Arrays.asList(78.456689, 12.822435))),
                    new Route.Stop("Govindapalle", 10, new GeoPoint("Point", Arrays.asList(78.453880, 12.813177))),
                    new Route.Stop("Lingapuram", 11, new GeoPoint("Point", Arrays.asList(78.449500, 12.802100))),
                    new Route.Stop("Ramalagutta Chenu", 12, new GeoPoint("Point", Arrays.asList(78.441200, 12.783400))),
                    new Route.Stop("Kangundhi", 13, new GeoPoint("Point", Arrays.asList(78.432970, 12.768058))),
                    new Route.Stop("Dase Gownur Crossing", 14, new GeoPoint("Point", Arrays.asList(78.388100, 12.752300))),
                    new Route.Stop("Kuppam Town Center", 15, new GeoPoint("Point", Arrays.asList(78.345572, 12.739798))),
                    new Route.Stop("Kuppam Engineering College (KEC - Terminus)", 16, new GeoPoint("Point", Arrays.asList(78.360311, 12.721662)))
            ));
            routeMDR.setActive(true);
            routeRepository.save(routeMDR);

            // Ensure Bus KEC-07 exists and is assigned to the driver
            Bus busKEC07 = busRepository.findByBusNumber("KEC-07").orElse(new Bus());
            busKEC07.setBusNumber("KEC-07");
            busKEC07.setRegistrationNumber("AP-39-TJ-2026");
            busKEC07.setRouteId(routeMDR.getId());
            if (driverUser != null) {
                busKEC07.setTrackerId(driverUser.getId());
            }
            busKEC07.setStatus(BusStatus.NOT_STARTED);
            busKEC07.setActive(true);
            busRepository.save(busKEC07);

            // Ensure location record exists for KEC-07 at Attikuppam origin
            BusLocation locKEC07 = busLocationRepository.findByBusId(busKEC07.getId()).orElse(new BusLocation());
            locKEC07.setBusId(busKEC07.getId());
            locKEC07.setLocation(new GeoPoint("Point", Arrays.asList(78.479812, 12.884713)));
            locKEC07.setAccuracy(8.0);
            locKEC07.setSpeed(0.0);
            locKEC07.setHeading(0.0);
            locKEC07.setUpdatedAt(Instant.now());
            busLocationRepository.save(locKEC07);

            System.out.println("Enforced single active bus KEC-07 and route 'Attikuppam → KEC (via MDR87)'.");

            createStudentUser("student@kec.ac.in", "Rohan Sharma", "22KEC401", "9888877777", CollegeType.ENGINEERING, Program.BTECH, Department.CSE, 3, "A", "2023 - 2027", routeMDR.getId(), busKEC07.getId());
            createStudentUser("student.btech.aiml@kec.ac.in", "Bhavana Reddy", "23KEC502", "9765432109", CollegeType.ENGINEERING, Program.BTECH, Department.CSE_AI_ML, 3, "B", "2023 - 2027", routeMDR.getId(), busKEC07.getId());
            createStudentUser("student.btech.ece@kec.ac.in", "Chaitanya Prasad", "24KEC603", "9654321098", CollegeType.ENGINEERING, Program.BTECH, Department.ECE, 2, "A", "2024 - 2028", routeMDR.getId(), busKEC07.getId());
            createStudentUser("student.bca@kec.ac.in", "Divya Sree", "23DEG005", "9543210987", CollegeType.DEGREE, Program.BCA, null, 3, "A", "2023 - 2026", routeMDR.getId(), busKEC07.getId());
            createStudentUser("student.bba@kec.ac.in", "Eshwar Naidu", "24DEG012", "9432109876", CollegeType.DEGREE, Program.BBA, null, 2, "B", "2024 - 2027", routeMDR.getId(), busKEC07.getId());
            createStudentUser("student.bcom@kec.ac.in", "Farooq Ahmed", "25DEG089", "9321098765", CollegeType.DEGREE, Program.BCOM, null, 1, "A", "2025 - 2028", routeMDR.getId(), busKEC07.getId());
            createStudentUser("student.dipece@kec.ac.in", "Hari Krishna", "23DIP031", "9210987654", CollegeType.DIPLOMA, Program.DIPLOMA, Department.ECE, 3, "A", "2023 - 2026", routeMDR.getId(), busKEC07.getId());
            createStudentUser("student.dipcse@kec.ac.in", "Ganesh Kumar", "24DIP122", "9109876543", CollegeType.DIPLOMA, Program.DIPLOMA, Department.CSE, 2, "B", "2024 - 2027", routeMDR.getId(), busKEC07.getId());
            createStudentUser("student.mba@kec.ac.in", "Indira Priyadarshini", "24MBA002", "9098765432", CollegeType.MBA, Program.MBA, null, 2, "A", "2024 - 2026", routeMDR.getId(), busKEC07.getId());

            System.out.println("Development students synchronized with KEC-07.");
            System.out.println("Database initialization completed successfully.");
        } catch (Exception e) {
            System.err.println("Database initialization warning (non-fatal): " + e.getMessage());
        }
    }

    private void createStudentUser(String email, String fullName, String studentId, String mobile,
                                   CollegeType collegeType, Program program, Department department, int year,
                                   String section, String batch, String routeId, String busId) {
        try {
            User user = userRepository.findByEmail(email).orElse(new User());
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode("password"));
            user.setRole(Role.STUDENT);
            user.setActive(true);
            userRepository.save(user);

            Student student = studentRepository.findByUserId(user.getId()).orElse(new Student());
            student.setUserId(user.getId());
            student.setFullName(fullName);
            student.setStudentId(studentId);
            student.setMobile(mobile);
            student.setCollegeType(collegeType);
            student.setProgram(program);
            student.setDepartment(department);
            student.setAcademicYear(year);
            student.setSection(section);
            student.setBatch(batch);
            student.setBoardingLocation(new GeoPoint("Point", Arrays.asList(78.479812, 12.884713)));
            student.setAssignedRoute(routeId);
            student.setAssignedBus(busId);
            studentRepository.save(student);
        } catch (Exception e) {
            System.out.println("Notice: student init for " + email + ": " + e.getMessage());
        }
    }
}
