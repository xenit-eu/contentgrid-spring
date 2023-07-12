package com.contentgrid.userapps.holmes.dcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.lang.String;
import java.util.UUID;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Case {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID id;

	private String name;

	private String description;

	@ManyToMany
	@JoinTable(name = "case__suspects", joinColumns = @JoinColumn(name = "case_id"), inverseJoinColumns = @JoinColumn(name = "person_id"))
	@JsonIgnore
	private List<Person> suspects;
}
