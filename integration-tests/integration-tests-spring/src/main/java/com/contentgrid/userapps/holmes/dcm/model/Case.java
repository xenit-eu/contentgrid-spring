package com.contentgrid.userapps.holmes.dcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.rest.core.annotation.RestResource;

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

	@ManyToOne
	@JoinColumn(name = "lead_detective")
	@JsonProperty("lead_detective")
	@RestResource(rel = "lead_detective", path = "lead-detective")
	private Person leadDetective;

	@OneToMany
	@JoinColumn(name = "_case_id__has_evidence")
	@JsonIgnore
	@RestResource(rel = "has_evidence", path = "has-evidence")
	private List<Evidence> hasEvidence;

	@OneToOne
	@JoinColumn(name = "scenario")
	private Evidence scenario;

}
