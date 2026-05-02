package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show;

@Repository
public interface IShowRepository extends JpaRepository<show, UUID> {}
