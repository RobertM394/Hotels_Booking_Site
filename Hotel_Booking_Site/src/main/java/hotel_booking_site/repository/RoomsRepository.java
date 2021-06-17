package hotel_booking_site.repository;

import java.util.List;
import java.sql.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hotel_booking_site.domain.Room;

import org.springframework.data.jpa.repository.Query;

public interface RoomsRepository extends JpaRepository<Room, String> {
	
	@Query
	List<Room> findAvailableRooms(String cityName, Date checkInDate, Date checkOutDate, int numberOccupants); 
	
	@Query
	List<Room> findByCityName(String cityName); 
	
	@Query(value="SELECT r.id, r.hotel_id, r.price_per_night, r.max_occupants, r.bed_type, r.number_of_beds FROM rooms r WHERE id = ?1", nativeQuery=true)
	Room findbyRoomId(int id);
	
}
