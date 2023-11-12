package com.mindex.challenge.service.impl;

import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.service.EmployeeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeServiceImplTest {

    private String employeeUrl;
    private String employeeIdUrl;
    private String reportingUrl;

    @Autowired
    private EmployeeService employeeService;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    public void setup() {
        employeeUrl = "http://localhost:" + port + "/employee";
        employeeIdUrl = "http://localhost:" + port + "/employee/{id}";
        reportingUrl = "http://localhost:" + port + "/employee/{id}/reporting";
    }

    @Test
    public void testCreateReadUpdate() {
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");

        // Create checks
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();

        assertNotNull(createdEmployee.getEmployeeId());
        assertEmployeeEquivalence(testEmployee, createdEmployee);

        // Read checks
        Employee readEmployee = restTemplate
                .getForEntity(employeeIdUrl, Employee.class, createdEmployee.getEmployeeId()).getBody();
        assertEquals(createdEmployee.getEmployeeId(), readEmployee.getEmployeeId());
        assertEmployeeEquivalence(createdEmployee, readEmployee);

        // Update checks
        readEmployee.setPosition("Development Manager");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Employee updatedEmployee = restTemplate.exchange(employeeIdUrl,
                HttpMethod.PUT,
                new HttpEntity<Employee>(readEmployee, headers),
                Employee.class,
                readEmployee.getEmployeeId()).getBody();

        assertEmployeeEquivalence(readEmployee, updatedEmployee);
    }

    // Note: I included one additional test here, but for a production app, each
    // endpoint should have associated tests
    @Test
    public void testGetReportingStructureByEmployeeId() throws Exception {

        // Build reporting structure: report2, report3 => report1 => rootEmp
        Employee report2 = new Employee();
        report2.setEmployeeId("report2");

        Employee report3 = new Employee();
        report3.setEmployeeId("report3");

        Employee report1 = new Employee();
        report1.setEmployeeId("report1");
        report1.setDirectReports(List.of(report2, report3));

        Employee rootEmp = new Employee();
        rootEmp.setEmployeeId("rootEmp");
        rootEmp.setDirectReports(List.of(report1));

        Employee createdReport2 = restTemplate.postForEntity(employeeUrl, report2, Employee.class).getBody();
        Employee createdReport3 = restTemplate.postForEntity(employeeUrl, report3, Employee.class).getBody();
        Employee createdReport1 = restTemplate.postForEntity(employeeUrl, report1, Employee.class).getBody();
        Employee createdRootEmp = restTemplate.postForEntity(employeeUrl, rootEmp, Employee.class).getBody();

        assertNotNull(createdReport2.getEmployeeId());
        assertNotNull(createdReport3.getEmployeeId());
        assertNotNull(createdReport1.getEmployeeId());
        assertNotNull(createdRootEmp.getEmployeeId());

        ReportingStructure reporting = restTemplate
                .getForEntity(reportingUrl, ReportingStructure.class, createdRootEmp.getEmployeeId()).getBody();
        assertEquals(createdRootEmp.getEmployeeId(), reporting.getEmployee().getEmployeeId());
        assertEquals(3, reporting.getNumberOfReports());
    }

    private static void assertEmployeeEquivalence(Employee expected, Employee actual) {
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getDepartment(), actual.getDepartment());
        assertEquals(expected.getPosition(), actual.getPosition());
    }
}
