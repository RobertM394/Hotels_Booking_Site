package hotel_booking_site;
/*References
 * https://stackoverflow.com/questions/41548771/spring-boot-test-for-controller-fails-with-404-code
 * https://stackoverflow.com/questions/24199393/how-to-send-a-mock-object-as-json-in-mockmvc
 * https://www.tutorialspoint.com/how-to-convert-java-object-to-json-using-jackson-library
 */
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.tomcat.jni.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import hotel_booking_site.services.*;
import hotel_booking_site.domain.*;
import hotel_booking_site.controllers.*;

@WebMvcTest(HotelsRestController.class)
public class HotelsRestControllerTest {
	
	//create MockBean stubs for services
	@MockBean 
	AvailableRoomsService availableRoomsService;
	
	@MockBean 
	NewBookingService newBookingService;
	
	@MockBean 
	CustomerDataService customerDataService;

	@Autowired
	private MockMvc mockMvc;
	
	//these objects will be initialized in the @BeforeEach
	private JacksonTester<List<RoomInfo>> jsonRoomInfoListAttempt;
	private JacksonTester<PackageBooking> jsonPackageBookingAttempt;
	private JacksonTester<Customer> jsonCustomerAttempt;
	
	@BeforeEach public void setUpEach() {
		MockitoAnnotations.initMocks(this);
		JacksonTester.initFields(this, new ObjectMapper());
	}
	
	/*Test Each REST Controller API*/
	@Test
	public void shouldReturnRoomResultsListOrNotFound() throws Exception {
		
		//Create Sql.Date parameters for Booking objects
		java.sql.Date checkIn = newBookingService.stringToSqlDate("06/01/2021");
		java.sql.Date checkOut = newBookingService.stringToSqlDate("06/07/2021");
		
		//build two RoomInfo objects for RoomInfo list
		Hotel hotel = new Hotel(1, "Hotel", "111 Avenue", "Monterey", "CA", 
				"USA", "10001", "777-777-7777", 5, "<img>", 5, "Pool", "Bay");
		Room room1 = new Room(1, 1, 199.0, 1, "Single", 1);
		Room room2 = new Room(2, 1, 299.0, 2, "Double", 2);		
		RoomInfo roomInfo1 = new RoomInfo(room1, hotel);
		RoomInfo roomInfo2 = new RoomInfo(room2, hotel);
		List<RoomInfo> roomInfoList = new ArrayList<>();
		roomInfoList.add(roomInfo1);
		roomInfoList.add(roomInfo2);
		
		//AvailableRoomsService MockBean will return roomInfoList when called 
		//by REST controller. If REST controller is working correctly, calling 
		//its API route should return roomInfoList in JSON format
		given(availableRoomsService.getAvailableRooms("Monterey", checkIn, checkOut, 1)).willReturn(roomInfoList);
		given(availableRoomsService.getAvailableRooms("InvalidCity", checkIn, checkOut, 1)).willReturn(null);
		
		//Simulate HTTP GET requests to API route for valid and invalid city names
		MockHttpServletResponse response = mockMvc.perform(get("/hotels/api/getRooms/?city=Monterey&checkInDate=06/01/2022&checkOutDate=06/07/2021&occupants=1")).andReturn().getResponse();
		MockHttpServletResponse notFoundResponse = mockMvc.perform(get("/hotels/api/getRooms/?city=InvalidCity&checkInDate=06/01/2022&checkOutDate=06/07/2021&occupants=1")).andReturn().getResponse();
		
		//Convert HTTP response in JSON to RoomInfo list and get status code of search when city name is not found
		List<RoomInfo> actualResult = jsonRoomInfoListAttempt.parseObject(response.getContentAsString());
		int invalidStatusCode = notFoundResponse.getStatus();
		
		//Compare actual and expected results
		assertEquals(actualResult, roomInfoList);
		assertEquals(404, invalidStatusCode);
	}
	
	@Test
	public void shouldPersistNewPackageBooking() throws Exception {
		
		//Create Sql.Date parameters for Booking objects
		java.sql.Date checkIn = newBookingService.stringToSqlDate("06/01/2021");
		java.sql.Date checkOut = newBookingService.stringToSqlDate("06/07/2021");
		
		//create PackageBooking object and convert to JSON
		PackageBooking packageBooking = new PackageBooking(1, 1, 1, 199.0, checkIn, checkOut, 5);
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(packageBooking);
		
		//Test stub for NewBookingService
		given(newBookingService.persistNewPackageBooking(packageBooking)).willReturn(true);
		
		//Call API with packageBooking in JSON form as POST parameter
		//API will only accept POST requests
		MockHttpServletResponse response = mockMvc.perform(post("/hotels/api/newPackageBooking/").contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse();
		MockHttpServletResponse invalidHttpMethodResponse = mockMvc.perform(get("/hotels/api/newPackageBooking/").contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse();
		int statusCode = response.getStatus();
		int invalidStatusCode = invalidHttpMethodResponse.getStatus();
		
		assertEquals(200, statusCode);
		assertEquals(405, invalidStatusCode);
	}

	@Test
	public void shouldDeletePackageBookingById() throws Exception {
		
		int packageBookingId = 123;
		
		//Return number of rows deleted in the database
		given(newBookingService.cancelPackageHotelBooking(packageBookingId)).willReturn(1);
		
		//Call API with id of PackageBooking to delete from database
		//Only delete (not post) method is accepted
		MockHttpServletResponse response = mockMvc.perform(delete("/hotels/api/cancelPackageBooking/123")).andReturn().getResponse();
		MockHttpServletResponse invalidHttpMethodResponse = mockMvc.perform(post("/hotels/api/cancelPackageBooking/123")).andReturn().getResponse();
		int statusCode = response.getStatus();
		int invalidStatusCode = invalidHttpMethodResponse.getStatus();
		
		assertEquals(200, statusCode);
		assertEquals(405, invalidStatusCode);
	}
	
	@Test
	public void shouldPersistNewCustomer() throws Exception {
		
		//create Customer object and convert to JSON
		Customer customer = new Customer("GivenName", "Surname", "email@email.com", "dc647eb65e6711e155375218212b3964", 150.0);
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(customer);
		
		given(customerDataService.persistNewCustomer(customer)).willReturn(true);
		
		//Call API to save Customer object
		MockHttpServletResponse response = mockMvc.perform(post("/hotels/api/createNewCustomer/").contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse();
		MockHttpServletResponse invalidHttpMethodResponse = mockMvc.perform(get("/hotels/api/createNewCustomer/").contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse();
		int statusCode = response.getStatus();
		int invalidStatusCode = invalidHttpMethodResponse.getStatus();
		
		assertEquals(200, statusCode);
		assertEquals(405, invalidStatusCode);
	}
	
}
