package com.carpool;

import com.carpool.entity.PassengerLocation;
import com.carpool.repository.PassengerLocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PassengerLocationRepositoryTest {

    @Autowired
    private PassengerLocationRepository repository;

    @Test
    void findFirstByPassengerIdOrderByUpdatedAtDesc_returnsMostRecentLocation() {
        UUID passengerId = UUID.randomUUID();

        PassengerLocation older = new PassengerLocation();
        older.setPassengerId(passengerId);
        older.setLatitude(12.34);
        older.setLongitude(56.78);
        older.setUpdatedAt(Instant.now().minusSeconds(120));
        repository.save(older);

        PassengerLocation newer = new PassengerLocation();
        newer.setPassengerId(passengerId);
        newer.setLatitude(98.76);
        newer.setLongitude(54.32);
        newer.setUpdatedAt(Instant.now());
        repository.save(newer);

        PassengerLocation result = repository.findFirstByPassengerIdOrderByUpdatedAtDesc(passengerId).orElseThrow();

        assertThat(result.getLatitude()).isEqualTo(98.76);
        assertThat(result.getLongitude()).isEqualTo(54.32);
    }
}
