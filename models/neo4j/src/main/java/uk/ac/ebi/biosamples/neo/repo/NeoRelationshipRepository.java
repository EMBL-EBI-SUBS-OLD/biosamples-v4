package uk.ac.ebi.biosamples.neo.repo;


import org.springframework.data.neo4j.repository.Neo4jRepository;

import uk.ac.ebi.biosamples.neo.model.NeoRelationship;

public interface NeoRelationshipRepository extends  Neo4jRepository<NeoRelationship,Long>{	
}
