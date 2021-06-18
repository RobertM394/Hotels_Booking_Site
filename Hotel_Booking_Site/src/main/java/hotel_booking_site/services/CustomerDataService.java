package hotel_booking_site.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import hotel_booking_site.domain.Customer;
import hotel_booking_site.repository.CustomersRepository;

@Service
public class CustomerDataService {
	
	@Autowired 
	CustomersRepository customersRepository;
	
	//password encoder
	BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
	public boolean persistNewCustomer(Customer customer) {
		
		String password = customer.getPassword();
		password = passwordEncoder.encode(password);
		customer.setPassword(password);
		
		customersRepository.save(customer);
		return true;
	}
	
	public Customer authenticateCustomer(String username, String password) {
		//authenticate password
		Customer customer = customersRepository.getCustomer(username);	
		if (customer != null) {
			String hashedPassword = customer.getPassword();
				if (passwordEncoder.matches(password, hashedPassword)) {
				return customer;
				}
		}
		return null;
	}
	
	public int findCustomerIdByUsername(String username) {
		Customer customer = customersRepository.findCustomerIdByUsername(username);
		
		if (customer == null) {
			return 0;
		}
		return customer.getId();
	}
	
	public Customer findCustomerById(int id) {
		Customer customer = customersRepository.findbyCustomerId(id);
		return customer;
	}
	
	public boolean updateAccountBalanceById(int id) {
		double balance = customersRepository.returnAccountBalanceById(id);
		customersRepository.updateAccountBalance(id, balance);
		return true;
	}

}

