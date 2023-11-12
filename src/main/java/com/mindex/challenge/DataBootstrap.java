package com.mindex.challenge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataBootstrap {
    private static final String DATASTORE_LOCATION = "/static/employee_database.json";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        InputStream inputStream = this.getClass().getResourceAsStream(DATASTORE_LOCATION);

        Employee[] employees = null;

        try {
            employees = objectMapper.readValue(inputStream, Employee[].class);
            materializeReferences(employees);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Employee employee : employees) {
            employeeRepository.insert(employee);
        }
    }

    private void materializeReferences(Employee[] employees)
    {
        Map<String, Employee> empMap = new HashMap<String, Employee>();
        for (int i = 0; i < employees.length; i++) {
            empMap.put(employees[i].getEmployeeId(), employees[i]);
        }

        for (Employee emp : employees) {            
            List<Employee> reports = emp.getDirectReports();

            if (reports != null) {                
                List<Employee> materialized = new ArrayList<Employee>();
                reports.forEach(report -> {
                    Employee ref = empMap.get(report.getEmployeeId());
                    materialized.add(ref);
                });
                emp.setDirectReports(materialized);
            }
        }
    }

}
