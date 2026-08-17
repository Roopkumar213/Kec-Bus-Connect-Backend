package com.kec.busconnect.service;

import com.kec.busconnect.dto.AuthResponse;
import com.kec.busconnect.dto.LoginRequest;
import com.kec.busconnect.dto.SignupRequest;
import com.kec.busconnect.enums.Role;
import com.kec.busconnect.enums.Program;
import com.kec.busconnect.enums.Department;
import com.kec.busconnect.enums.CollegeType;
import com.kec.busconnect.exception.BadRequestException;
import com.kec.busconnect.model.GeoPoint;
import com.kec.busconnect.model.Student;
import com.kec.busconnect.model.User;
import com.kec.busconnect.repository.StudentRepository;
import com.kec.busconnect.repository.UserRepository;
import com.kec.busconnect.security.JwtService;
import com.kec.busconnect.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private static final Map<CollegeType, List<Program>> COLLEGE_PROGRAMS = Map.of(
            CollegeType.ENGINEERING, List.of(Program.BTECH),
            CollegeType.DEGREE, List.of(Program.BCA, Program.BBA, Program.BCOM),
            CollegeType.DIPLOMA, List.of(Program.DIPLOMA),
            CollegeType.MBA, List.of(Program.MBA)
    );

    private static final Map<Program, List<Department>> PROGRAM_DEPARTMENTS = Map.of(
            Program.BTECH, List.of(Department.CSE, Department.CSE_AI_ML, Department.CSE_DS, Department.ECE, Department.EEE, Department.MECHANICAL, Department.CIVIL),
            Program.DIPLOMA, List.of(Department.CSE, Department.ECE, Department.MECHANICAL, Department.EEE)
    );

    private static final Map<Program, Integer> PROGRAM_MAX_YEARS = Map.of(
            Program.BTECH, 4,
            Program.BCA, 3,
            Program.BBA, 3,
            Program.BCOM, 3,
            Program.DIPLOMA, 3,
            Program.MBA, 2
    );

    public AuthService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public void registerStudent(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new BadRequestException("Email already exists");
        }

        if (studentRepository.existsByStudentId(request.getStudentId().trim())) {
            throw new BadRequestException("Student ID already exists");
        }

        validateAcademicStructure(request.getCollegeType(), request.getProgram(), request.getDepartment(), request.getAcademicYear());

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT);
        user.setActive(true);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUserId(user.getId());
        student.setFullName(request.getFullName().trim());
        student.setStudentId(request.getStudentId().trim());
        student.setMobile(request.getMobile().trim());
        student.setCollegeType(request.getCollegeType());
        student.setProgram(request.getProgram());
        student.setDepartment(request.getDepartment());
        student.setAcademicYear(request.getAcademicYear());
        student.setSection(request.getSection() != null ? request.getSection().trim() : null);
        student.setBatch(request.getBatch() != null ? request.getBatch().trim() : null);

        GeoPoint location = new GeoPoint(
                "Point",
                Arrays.asList(request.getBoardingLocation().getLongitude(), request.getBoardingLocation().getLatitude())
        );
        student.setBoardingLocation(location);
        
        student.setAssignedBus(request.getAssignedBus());
        student.setAssignedRoute(request.getAssignedRoute());

        studentRepository.save(student);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = principal.getUser();
        String token = jwtService.generateToken(principal);

        String roleName = (user.getRole() == Role.TRACKER || user.getRole() == Role.DRIVER) ? "DRIVER" : user.getRole().name();
        AuthResponse.UserDto userDto = new AuthResponse.UserDto(
                user.getId(),
                user.getEmail(),
                roleName
        );

        return new AuthResponse(true, "Login successful", token, userDto);
    }

    private void validateAcademicStructure(CollegeType type, Program program, Department dept, int year) {
        List<Program> allowedPrograms = COLLEGE_PROGRAMS.get(type);
        if (allowedPrograms == null || !allowedPrograms.contains(program)) {
            throw new BadRequestException(program + " is not a valid program for college type " + type);
        }

        List<Department> allowedDepts = PROGRAM_DEPARTMENTS.get(program);
        if (allowedDepts != null) {
            if (dept == null || !allowedDepts.contains(dept)) {
                throw new BadRequestException("Department " + dept + " is required and must be valid for program " + program);
            }
        } else {
            if (dept != null) {
                throw new BadRequestException("Department is not supported for program " + program);
            }
        }

        Integer maxYears = PROGRAM_MAX_YEARS.get(program);
        if (maxYears != null && year > maxYears) {
            throw new BadRequestException("Academic year " + year + " exceeds the maximum years (" + maxYears + ") for program " + program);
        }
    }
}
