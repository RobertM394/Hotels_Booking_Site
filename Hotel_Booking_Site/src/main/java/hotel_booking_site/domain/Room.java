package hotel_booking_site.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;

@Entity
@Table(name="rooms")
/* Custom Queries
 * 
 * References:
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#reference
 * https://www.javaguides.net/2018/11/spring-data-jpa-namednativequery-namednativequeries-example.html 
 * 
 * @NamedNativeQuery findAvailableRooms finds all available rooms not already booked during the user's searched date range
 * Compares against both bookings and package_bookings table
 */
@NamedNativeQuery(name = "Room.findAvailableRooms",
	query = "SELECT r.id, r.hotel_id, r.price_per_night, r.max_occupants, r.bed_type, r.number_of_beds "
			+ "FROM rooms r JOIN hotels_table h ON r.hotel_id = h.id WHERE h.city = ?1 AND ?4 <= r.max_occupants "
			+ "AND r.id NOT IN (SELECT b.room_id FROM bookings b WHERE"
			+ "(?2 >= b.check_in_date AND ?2 < b.check_out_date)"
			+ "OR"
			+ "(?2 <= b.check_in_date AND ?3 > b.check_in_date)"
			+ ") "
			+ "AND r.id NOT IN (SELECT p.room_id FROM package_bookings p WHERE"
			+ "(?2 >= p.check_in_date AND ?2 < p.check_out_date)"
			+ "OR"
			+ "(?2 <= p.check_in_date AND ?3 > p.check_in_date)"
			+ ") "
			+ "ORDER BY r.price_per_night",
			resultClass = Room.class)

@NamedNativeQuery(name = "Room.findByCityName",
query = "SELECT r.id, r.hotel_id, r.price_per_night, r.max_occupants, r.bed_type, r.number_of_beds FROM rooms r JOIN hotels_table h ON r.hotel_id = h.id WHERE h.city = ?1 AND r.id NOT IN (SELECT room_id FROM bookings) ORDER BY r.price_per_night",
		resultClass = Room.class)

public class Room {
	
	@Id
	private int id;
	private int hotel_id;
	private double price_per_night;
	private int max_occupants;
	private String bed_type;
	private int number_of_beds;
	
	public Room () { }

	public Room(int id, int hotel_id, double price_per_night, int max_occupants, String bed_type, int number_of_beds)
	{
		super();
		this.id = id;
		this.hotel_id = hotel_id;
		this.price_per_night = price_per_night;
		this.max_occupants = max_occupants;
		this.bed_type = bed_type;
		this.number_of_beds = number_of_beds;
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public int getHotel_id()
	{
		return hotel_id;
	}

	public void setHotel_id(int hotel_id)
	{
		this.hotel_id = hotel_id;
	}

	public double getPrice_per_night()
	{
		return price_per_night;
	}

	public void setPrice_per_night(double price_per_night)
	{
		this.price_per_night = price_per_night;
	}

	public int getMax_occupants()
	{
		return max_occupants;
	}

	public void setMax_occupants(int max_occupants)
	{
		this.max_occupants = max_occupants;
	}

	public String getBed_type()
	{
		return bed_type;
	}

	public void setBed_type(String bed_type)
	{
		this.bed_type = bed_type;
	}

	public int getNumber_of_beds()
	{
		return number_of_beds;
	}

	public void setNumber_of_beds(int number_of_beds)
	{
		this.number_of_beds = number_of_beds;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bed_type == null) ? 0 : bed_type.hashCode());
		result = prime * result + hotel_id;
		result = prime * result + id;
		result = prime * result + max_occupants;
		result = prime * result + number_of_beds;
		long temp;
		temp = Double.doubleToLongBits(price_per_night);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Room other = (Room) obj;
		if (bed_type == null)
		{
			if (other.bed_type != null)
				return false;
		} else if (!bed_type.equals(other.bed_type))
			return false;
		if (hotel_id != other.hotel_id)
			return false;
		if (id != other.id)
			return false;
		if (max_occupants != other.max_occupants)
			return false;
		if (number_of_beds != other.number_of_beds)
			return false;
		if (Double.doubleToLongBits(price_per_night) != Double.doubleToLongBits(other.price_per_night))
			return false;
		return true;
	}

}
