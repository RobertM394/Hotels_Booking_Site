package hotel_booking_site;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import hotel_booking_site.domain.Customer;
import hotel_booking_site.repository.CustomersRepository;
import hotel_booking_site.services.CustomerDataService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class CustomerDataServiceTest {
	
	@MockBean
	CustomersRepository customersRepository;
	
	@Autowired
	CustomerDataService customerDataService;
	
	@BeforeEach
	public void setUpEach() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void customerDataServiceShouldPersistNewCustomer() {
	
	Customer customer = new Customer("firstName", "lastName", "email@email.com", "password", 0);
	given(customersRepository.save(customer)).willReturn(customer); 
	
	//CustomerDataService returns true if customer was successfully saved to database
	assertThat(true).isEqualTo(customerDataService.persistNewCustomer(customer));
	}
	
	@Test
	public void authenticateCustomerShouldReturnCustomerObjectOrNull() {
		
		//Encrypt customer password so it matches hashed password comparison in authentication method in CustomerDataService
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		
		Customer customer = new Customer("firstName", "lastName", "username@email.com", "password", 0);
		String password = customer.getPassword();
		String hashedPassword = passwordEncoder.encode(password);
		customer.setPassword(hashedPassword);
		
		given(customersRepository.getCustomer("username@email.com")).willReturn(customer);
		given(customersRepository.getCustomer("invalidUsername@email.com")).willReturn(null);
		
		Customer expectedResult = customer;
		Customer actualResult = customerDataService.authenticateCustomer("username@email.com", "password");
		Customer invalidCustomerResult = customerDataService.authenticateCustomer("invalidUsername@email.com", "password");
		
		assertEquals(expectedResult, actualResult); 
		assertEquals(null, invalidCustomerResult);
	}
	
	@Test
	public void shouldReturnCustomerIdGivenUsername() {
		Customer customer = new Customer("firstName", "lastName", "username@email.com", "password", 0);
		customer.setId(1);
		
		given(customersRepository.findCustomerIdByUsername("username@email.com")).willReturn(customer);
		given(customersRepository.findCustomerIdByUsername("invalidUsername@email.com")).willReturn(null);
		
		int id = customerDataService.findCustomerIdByUsername("username@email.com");
		int invalidResult = customerDataService.findCustomerIdByUsername("invalidUsername@email.com");
		
		assertEquals(1, id);
		assertEquals(0, invalidResult);
	}
	
	@Test 
	public void shouldReturnValidCustomerById() {
		
		Customer customer = new Customer("firstName", "lastName", "username@email.com", "password", 0);
		customer.setId(1);
		
		given(customersRepository.findbyCustomerId(1)).willReturn(customer);
		given(customersRepository.findbyCustomerId(2)).willReturn(null);
		
		Customer expectedResult = customer;
		Customer actualResult = customerDataService.findCustomerById(1);
		Customer invalidActualResult = customerDataService.findCustomerById(2);
		
		assertEquals(expectedResult, actualResult);
		assertEquals(null, invalidActualResult);
	}
	
	@Test
	public void updateCustomerAccountBalanceShouldReturnTrue() {
		
		given(customersRepository.returnAccountBalanceById(1)).willReturn(100.00);
		//Invalid search ID returns 0 instead of null
		given(customersRepository.returnAccountBalanceById(0)).willReturn(0.0); 
		
		//Method will return true if update is successful or not found but query ran OK
		boolean expectedResult = true;
		boolean invalidExpectedResult = true;
		
		boolean actualResult = customerDataService.updateAccountBalanceById(1);
		boolean invalidActualResult = customerDataService.updateAccountBalanceById(0);
		
		assertEquals(expectedResult, actualResult);
		assertEquals(invalidExpectedResult, invalidActualResult);
	}
	
}
