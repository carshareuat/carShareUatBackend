package com.carpool.config;

import com.carpool.entity.Location;
import com.carpool.repository.LocationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LocationDataInitializer {

    private final LocationRepository locationRepository;

    @PostConstruct
    @Transactional
    public void init() {
        if (locationRepository.count() > 0) return;
        List<Location> list = new ArrayList<>();

        // Kerala
        String state = "Kerala";
        String[] kerala = {"Thiruvananthapuram","Kollam","Pathanamthitta","Alappuzha","Kottayam","Idukki","Ernakulam","Thrissur","Palakkad","Malappuram","Kozhikode","Wayanad","Kannur","Kasaragod"};
        for (String d : kerala) { Location l = new Location(); l.setState(state); l.setDistrict(d); list.add(l); }

        // Tamil Nadu
        state = "Tamil Nadu";
        String[] tamil = {"Chennai","Kanchipuram","Chengalpattu","Tiruvallur","Vellore","Tirupathur","Tiruvannamalai","Villupuram","Kallakurichi","Cuddalore","Nagapattinam","Mayiladuthurai","Thanjavur","Tiruvarur","Pudukkottai","Sivaganga","Ramanathapuram","Virudhunagar","Thoothukudi","Tirunelveli","Kanyakumari","Madurai","Dindigul","Theni","Tiruchirappalli","Karur","Perambalur","Ariyalur","Salem","Namakkal","Erode","Coimbatore","Tiruppur","Krishnagiri","Dharmapuri"};
        for (String d : tamil) { Location l = new Location(); l.setState(state); l.setDistrict(d); list.add(l); }

        // Andhra Pradesh
        state = "Andhra Pradesh";
        String[] ap = {"Anantapur","Chittoor","Tirupati","Nellore","Prakasam","Guntur","Bapatla","NTR","Palnadu","Vijayawada","Krishna","Kurnool","Nandyal","Kakinada","East Godavari","West Godavari","Srikakulam","Vizianagaram","Visakhapatnam"};
        for (String d : ap) { Location l = new Location(); l.setState(state); l.setDistrict(d); list.add(l); }

        // Karnataka
        state = "Karnataka";
        String[] karnataka = {"Bengaluru Urban","Bengaluru Rural","Mysuru","Mandya","Chamarajanagar","Hassan","Chikkamagaluru","Udupi","Dakshina Kannada","Kodagu","Shivamogga","Tumakuru","Chitradurga","Davanagere","Ballari","Koppal","Raichur","Gadag","Haveri","Bagalkot","Bidar","Kalaburagi","Vijayapura","Belagavi","Dharwad","Uttara Kannada","Ramanagara","Chikkaballapur","Kolar","Yadgir"};
        for (String d : karnataka) { Location l = new Location(); l.setState(state); l.setDistrict(d); list.add(l); }

        // Puducherry
        state = "Puducherry";
        String[] pud = {"Puducherry","Karaikal","Mahe","Yanam"};
        for (String d : pud) { Location l = new Location(); l.setState(state); l.setDistrict(d); list.add(l); }

        locationRepository.saveAll(list);
    }
}
