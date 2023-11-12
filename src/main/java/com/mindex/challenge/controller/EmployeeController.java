package com.mindex.challenge.controller;

import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.service.EmployeeService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class EmployeeController {
    private static final Logger LOG = LoggerFactory.getLogger(EmployeeController.class);

    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/employee")
    public Employee create(@RequestBody Employee employee) {
        LOG.debug("Received employee create request for [{}]", employee);

        return employeeService.create(employee);
    }

    @GetMapping("/employee/{id}")
    public Employee read(@PathVariable String id) {
        LOG.debug("Received employee create request for id [{}]", id);

        return employeeService.read(id);
    }

    @PutMapping("/employee/{id}")
    public Employee update(@PathVariable String id, @RequestBody Employee employee) {
        LOG.debug("Received employee create request for id [{}] and employee [{}]", id, employee);

        employee.setEmployeeId(id);
        return employeeService.update(employee);
    }

    @GetMapping("/employee/{id}/reporting")
    public ResponseEntity<ReportingStructure> getReportingStructureByEmployeeId(@PathVariable String id) {
        LOG.debug("Received reporting structure get request for employee id [{}]", id);

        Employee employee;
        try {
            employee = employeeService.read(id);
        } catch (Exception ex) {
            return new ResponseEntity<ReportingStructure>(HttpStatus.NOT_FOUND);
        }

        int numReports = 0;
        List<Employee> directReports = Optional.ofNullable(employee.getDirectReports())
                .orElse(new ArrayList<Employee>());
        Queue<Employee> reportQueue = new ArrayDeque<Employee>(directReports);
        while (reportQueue.size() > 0) {
            Employee emp = reportQueue.remove();
            numReports++;
            if (emp.getDirectReports() != null) {
                for (Employee report : emp.getDirectReports()) {
                    reportQueue.add(report);
                }
            }
        }

        int finalNumReports = numReports;
        ReportingStructure reportingStructure = new ReportingStructure();
        reportingStructure.setEmployee(employee);
        reportingStructure.setNumberOfReports(finalNumReports);
        return new ResponseEntity<ReportingStructure>(reportingStructure, HttpStatus.OK);
    }

    @GetMapping("/employee/{id}/compensation")
    public ResponseEntity<Compensation> getCompensationByEmployeeId(@PathVariable String id) {
        LOG.debug("Received compensation get create request for employee id [{}]", id);

        Compensation compensation = employeeService.getCompensationByEmployeeId(id);
        if (compensation == null) {
            return new ResponseEntity<Compensation>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Compensation>(compensation, HttpStatus.OK);
    }

    @PostMapping("/employee/{id}/compensation")
    public ResponseEntity<Compensation> createCompensation(@PathVariable String id,
            @RequestBody Compensation compensation) {
        LOG.debug("Received compensation create request for employee id [{}]", id);

        Employee employee = employeeService.read(id);
        if (employee == null) {
            return new ResponseEntity<Compensation>(HttpStatus.NOT_FOUND);
        }

        // Only allow one compensation per employee. In a production app, we might
        // implement a PUT endpoint for updating it.
        Compensation existingCompensation = employeeService.getCompensationByEmployeeId(id);
        if (existingCompensation != null) {
            return new ResponseEntity<Compensation>(HttpStatus.BAD_REQUEST);
        }

        compensation.setEmployee(employee);

        employeeService.createCompensation(compensation);
        return new ResponseEntity<Compensation>(compensation, HttpStatus.CREATED);
    }
}
