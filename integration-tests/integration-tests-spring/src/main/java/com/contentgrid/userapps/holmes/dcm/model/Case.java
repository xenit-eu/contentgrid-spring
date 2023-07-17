package com.contentgrid.userapps.holmes.dcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
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
