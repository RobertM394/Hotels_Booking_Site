package hotel_booking_site.controllers;
/* References
 * https://stackoverflow.com/questions/10413350/date-conversion-from-string-to-sql-date-in-java-giving-different-output/17673514
 * 
 */
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;
import javax.websocket.Session;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import hotel_booking_site.domain.Booking;
import hotel_booking_site.domain.BookingInfo;
import hotel_booking_site.domain.Customer;
import hotel_booking_site.domain.RoomInfo;
import hotel_booking_site.services.AvailableRoomsService;
import hotel_booking_site.services.CustomerDataService;
import hotel_booking_site.services.NewBookingService;
import hotel_booking_site.repository.CustomersRepository;

@Controller
public class HotelBookingController {
		
	@Autowired 
	AvailableRoomsService availableRoomsService;
	
	@Autowired
	NewBookingService newBookingService;
	
	@Autowired
	CustomerDataService customerDataService;
	
	@Autowired
	CustomersRepository customersRepository;
	
	@Autowired 
	HttpSession session;
	
	java.sql.Date sqlCheckInDate;
	java.sql.Date sqlCheckOutDate;
	
	//HTTP  Routes
	@GetMapping("/hotels/home")
	public String getHotelsHomepage(Model model) {
		return "hotels_homepage";
	}
	
	@PostMapping("/hotels/results")
	public String getHotelSearchResults(Model model, HttpSession session,
			@RequestParam("city") String city,
			@RequestParam("checkInDate") String checkInDate,
			@RequestParam("checkOutDate") String checkOutDate,
			@RequestParam("occupants") int numberOccupants
			) throws ParseException {
		
		sqlCheckInDate = newBookingService.stringToSqlDate(checkInDate);
		sqlCheckOutDate = newBookingService.stringToSqlDate(checkOutDate);
		
		//Add search parameters to HttpSession and Model
		session.setAttribute("city", city);
		session.setAttribute("checkInDate",  sqlCheckInDate);
		session.setAttribute("checkOutDate", sqlCheckOutDate);
		session.setAttribute("occupants", numberOccupants);
		
		model.addAttribute("city", city);
		model.addAttribute("checkInDate", sqlCheckInDate);
		model.addAttribute("checkOutDate", sqlCheckOutDate);
		model.addAttribute("numberOccupants", numberOccupants);
		
		//Query database to find available rooms
		List<RoomInfo> roomInfoList = availableRoomsService.getAvailableRooms(city, sqlCheckInDate, sqlCheckOutDate, numberOccupants);
		
		if (roomInfoList == null) {
			String no_rooms_found = "Sorry, no available rooms where found. Please try "
					+ "a different city or date.";
			model.addAttribute("no_results", true);
			model.addAttribute("no_rooms_found", no_rooms_found);
			return "hotels_homepage";
		}
		else {
		System.out.println("Search successful.");
		model.addAttribute(roomInfoList);
		return "hotel_results_page";
		}
	}
	
	@PostMapping("/hotels/checkout")
	public String getCheckoutDetails(Model model, HttpSession session,
			@RequestParam("roomId") int roomId,
			@RequestParam("pricePerNight") double pricePerNight
			) {
		
		//Calculate total price of booking
		java.util.Date checkIn = (java.util.Date) session.getAttribute("checkInDate");
		java.util.Date checkOut = (java.util.Date) session.getAttribute("checkOutDate");
		long diffInMilliseconds = Math.abs(checkOut.getTime() - checkIn.getTime());
		long reservationDays = TimeUnit.DAYS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
		double totalPrice = pricePerNight * reservationDays;
		
		session.setAttribute("totalPrice", totalPrice);
		session.setAttribute("roomId", roomId);
	
		model.addAttribute(totalPrice);
		model.addAttribute("roomId", roomId); 
		model.addAttribute("city", session.getAttribute("city"));
		model.addAttribute("checkInDate", session.getAttribute("checkInDate"));
		model.addAttribute("checkOutDate", session.getAttribute("checkOutDate"));
		
		if (session.getAttribute("authenticated") != null) {
			Boolean authenticated = (Boolean) session.getAttribute("authenticated");
			if (authenticated) {
				return "existing_customer_checkout";
			}	
		}
		return "new_customer_checkout_page";
	}
	
	@PostMapping("/hotels/submitNewCustomerBooking")
	public String persistBookingAndCustomerToDatabase(Model model, HttpSession session,
			//new booking parameters
			@RequestParam("roomId") int roomId,
			@RequestParam("numberOccupants") int numberOccupants,
			
			//new customer parameters
			@RequestParam("firstName") String first_name,
			@RequestParam("lastName") String last_name,
			@RequestParam("email") String email,
			@RequestParam("password") String password
			) {
		
		double totalPrice = (Double) session.getAttribute("totalPrice");
		model.addAttribute("totalPrice", totalPrice);
		model.addAttribute("checkInDate", session.getAttribute("checkInDate"));
		model.addAttribute("checkOutDate", session.getAttribute("checkOutDate"));
		model.addAttribute("numberOccupants", session.getAttribute("occupants"));
		
		//Persist new customer and booking to the database
		Customer customer = new Customer();
		customer.setFirst_name(first_name);
		customer.setLast_name(last_name);
		customer.setEmail(email);
		customer.setPassword(password);
		customer.setCurrent_balance(totalPrice);
		customerDataService.persistNewCustomer(customer);
		
		Booking booking = new Booking();
		booking.setRoom_id(roomId);
		booking.setCustomer_id(customerDataService.findCustomerIdByUsername(email));
		booking.setCheck_in_date(sqlCheckInDate);
		booking.setCheck_out_date(sqlCheckOutDate);
		booking.setTotal_price(totalPrice);
		booking.setNumber_occupants(numberOccupants);	
		newBookingService.persistNewBooking(booking);
		
		model.addAttribute(customer);
		
		System.out.println("New Customer " + booking.getCustomer_id() + 
						   " booked room " + booking.getRoom_id() +
				           " successfully.");
		
		return "booking_confirmation_page";
	}
	
	@PostMapping("/hotels/submitExistingCustomerBooking")
	public String persistBookingToDatabase(Model model, HttpSession session,
			//new booking parameters
			@RequestParam("roomId") int roomId
			) {
		
		Customer customer = (Customer) session.getAttribute("customer");
		model.addAttribute("checkInDate", session.getAttribute("checkInDate"));
		model.addAttribute("checkOutDate", session.getAttribute("checkOutDate"));
		model.addAttribute("totalPrice", session.getAttribute("totalPrice"));
		model.addAttribute("numberOccupants", session.getAttribute("occupants"));
		
		//Persist new booking to database
		Double totalPrice = (Double) session.getAttribute("totalPrice");
		int numberOccupants = (int) session.getAttribute("occupants");
		Booking booking = new Booking();
		booking.setRoom_id(roomId);
		booking.setCustomer_id(customer.getId());
		booking.setCheck_in_date(sqlCheckInDate);
		booking.setCheck_out_date(sqlCheckOutDate);
		booking.setTotal_price(totalPrice);
		booking.setNumber_occupants(numberOccupants);	
		newBookingService.persistNewBooking(booking);
		
		//Update customer account balance and bookings and return account page
		customerDataService.updateAccountBalanceById(customer.getId());
		double currentBalance = customersRepository.returnAccountBalanceById(customer.getId());
		customer.setCurrent_balance(currentBalance);
		model.addAttribute(customer);
		
		System.out.println("Existing user " + booking.getCustomer_id() + 
						   " booked room " + booking.getRoom_id() +
						   " successfully.");
		
		List<BookingInfo> bookingInfoList = newBookingService.getListOfBookingsByCustomerId(customer.getId());
		if (bookingInfoList == null) {
			model.addAttribute("bookings", false);
		}
		else {
		model.addAttribute("bookings", true);
		model.addAttribute(bookingInfoList);
		}
		return "customer_account_page";
	}
	
	@PostMapping("/hotels/myAccount")
	public String getCustomerAccountDetails(Model model, HttpSession session,
			@RequestParam("username") String username,
			@RequestParam("password") String password
			) {
		
		System.out.println("User logged in successfully.");
		
		Customer customer =	customerDataService.authenticateCustomer(username, password);
		if (customer == null) {
			model.addAttribute("invalid_login", true);
			return "customer_login_page";
		}
		else {

		//Update customer account balance
		customerDataService.updateAccountBalanceById(customer.getId());
		double currentBalance = customersRepository.returnAccountBalanceById(customer.getId());
		customer.setCurrent_balance(currentBalance); 
		
		session.setAttribute("authenticated", true);
		session.setAttribute("customer", customer);
		model.addAttribute(customer);

		//Build list of customer's bookings if login is valid
		List<BookingInfo> bookingInfoList = newBookingService.getListOfBookingsByCustomerId(customer.getId());
		if (bookingInfoList == null) {
			model.addAttribute("bookings", false);
		}
		
		else {
		model.addAttribute("bookings", true);
		model.addAttribute(bookingInfoList);
		}
		
		return "customer_account_page";
		}
	}
	
	//Hide URL params
	@GetMapping("/hotels/myAccount")
	public String getCustomerAccountPage(Model model, HttpSession session) {
		Customer customer = (Customer) session.getAttribute("customer");

		//Update customer account balance
		customerDataService.updateAccountBalanceById(customer.getId());
		double currentBalance = customersRepository.returnAccountBalanceById(customer.getId());
		customer.setCurrent_balance(currentBalance); 

		model.addAttribute(customer);

		//Build list of customer's bookings
		List<BookingInfo> bookingInfoList = newBookingService.getListOfBookingsByCustomerId(customer.getId());
		if (bookingInfoList == null) {
			model.addAttribute("bookings", false);
		}

		else {
		model.addAttribute("bookings", true);
		model.addAttribute(bookingInfoList);
		}


		return "customer_account_page";
	}
	
	@PostMapping("/hotels/cancelHotelBooking")
	public String cancelHotelBooking(Model model, 
			@RequestParam("booking_id") int booking_id,
			@RequestParam("customer_id") int customer_id
			){
		
		//cancel hotel booking
		newBookingService.cancelHotelBooking(booking_id);
		
		//Retrieve updated list of customer bookings
		//Update customer account balance
		Customer customer = customerDataService.findCustomerById(customer_id);
		customerDataService.updateAccountBalanceById(customer.getId());
		double currentBalance = customersRepository.returnAccountBalanceById(customer.getId());
		customer.setCurrent_balance(currentBalance); 
		model.addAttribute(customer);
		System.out.println("Customer " + customer_id + " cancelled booking " + booking_id);
		
		List<BookingInfo> bookingInfoList = newBookingService.getListOfBookingsByCustomerId(customer.getId());
		if (bookingInfoList == null) {
			model.addAttribute("bookings", false);
		}
		
		else {
		model.addAttribute("bookings", true);
		model.addAttribute(bookingInfoList);
		}
		
		return "customer_account_page";
	}
	
	//Customer logout
	@GetMapping("hotels/logout")
	public String removeCustomerSession(HttpSession session) {
		session.setAttribute("authenticated", false);
		session.invalidate();
		System.out.println("User logged out succesfully.");
		return "customer_login_page";
	}
	
	//Navigation Bar Routes
	@GetMapping("/hotels/login")
	public String getHotelsLoginPage(Model model) {
		return "customer_login_page";
	}
	
	@GetMapping("/hotels/signup")
	public String getHotelsSignUpPage(Model model) {
		return "hotels_signup_page";
	}
	
	@GetMapping("/airline")
	public String getAirlineHomepage(Model model) {
		return "airline";
	}
	
	@GetMapping("/carrentals")
	public String getCarRentalsHomepage(Model model) {
		return "car_rentals";
	}
	
	@GetMapping("/deals")
	public String getDealsHomepage(Model model) {
		return "deals";
	}
	
}

/*
 * References: 
 * https://stackoverflow.com/questions/49333907/how-to-set-session-attributes-in-spring-boot
 * https://www.baeldung.com/java-date-difference
 */
