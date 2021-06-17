package hotel_booking_site.services;


import java.util.List;
import java.util.ArrayList;
import java.sql.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import hotel_booking_site.domain.Hotel;
import hotel_booking_site.domain.Room;
import hotel_booking_site.domain.RoomInfo;
import hotel_booking_site.repository.HotelsRepository;
import hotel_booking_site.repository.RoomsRepository;

@Service
public class AvailableRoomsService {
	
	@Autowired
	private RoomsRepository roomsRepository;
	
	@Autowired
	private HotelsRepository hotelsRepository;
	
	//retrieve a list of RoomInfo objects with room and hotel information
	//representing the available rooms according to the user's search parameters
	public List<RoomInfo> getAvailableRooms(String cityName, Date checkInDate, Date checkOutDate, int numberOccupants) {
		
		List<RoomInfo> roomInfoList = new ArrayList<>();
		List<Room> rooms = roomsRepository.findAvailableRooms(cityName, checkInDate, checkOutDate, numberOccupants);
		
		if (rooms.size() < 1) {
			return null;
		}
		
		else {
			for (Room room : rooms) {
				
				//build RoomInfo object
				Hotel hotel = hotelsRepository.findById(room.getHotel_id());
				RoomInfo roomInfo = new RoomInfo(room, hotel);
				roomInfoList.add(roomInfo);
				}
		}
		
		return roomInfoList;
	}
	
	//retrieve a list of RoomInfo objects with room and hotel information
	//representing the available rooms according to the user's search parameters
	public List<RoomInfo> getRoomInfo(String cityName) {
		
		List<RoomInfo> roomInfoList = new ArrayList<>();
		List<Room> rooms = roomsRepository.findByCityName(cityName);
		
		if (rooms.size() < 1) {
			return null;
		}
		
		else {
			for (Room room : rooms) {
				
				//build RoomInfo object
				Hotel hotel = hotelsRepository.findById(room.getHotel_id());
				RoomInfo roomInfo = new RoomInfo(room, hotel);
				roomInfoList.add(roomInfo);
				}
		}
		
		return roomInfoList;
	}
	
	
	
	
}
