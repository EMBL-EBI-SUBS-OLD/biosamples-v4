package uk.ac.ebi.biosamples.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.security.UserAuthentication;

@Service
public class BioSamplesAapService {

	
	private Logger log = LoggerFactory.getLogger(getClass());


	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample must specify a domain") // 400
	public static class SampleDomainMissingException extends RuntimeException {
	}

	@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Sample not accessible") // 403
	public static class SampleNotAccessibleException extends RuntimeException {
	}
	
	public Set<String> getDomains() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		log.info("authentication = "+authentication);

		//not sure this can ever happen
		if (authentication == null) {
			return Collections.emptySet();

		} else if (authentication instanceof AnonymousAuthenticationToken) {
			return Collections.emptySet();
		} else if (authentication instanceof UserAuthentication) {
			UserAuthentication userAuthentication = (UserAuthentication) authentication;
					
			log.info("userAuthentication = "+userAuthentication.getName());
			log.info("userAuthentication = "+userAuthentication.getAuthorities());
			log.info("userAuthentication = "+userAuthentication.getPrincipal());
			log.info("userAuthentication = "+userAuthentication.getCredentials());
			
			Set<String> domains = new HashSet<>();
			for (GrantedAuthority authority : authentication.getAuthorities()) {
				if (authority instanceof Domain) {
					log.info("Found domain "+authority);
					Domain domain = (Domain) authority;
	
					log.info("domain.getDomainName() = "+domain.getDomainName());
					log.info("domain.getDomainReference() = "+domain.getDomainReference());
					
					//NOTE this should use reference, but that is not populated in the tokens at the moment
					//domains.add(domain.getDomainReference());
					domains.add(domain.getDomainName());
				} else {
					log.info("Found non-domain "+authority);
				}
			}
			return domains;
		} else {
			return Collections.emptySet();
		}
	}
	
	public Sample handleDomain(Sample sample) throws SampleNotAccessibleException,SampleNotAccessibleException {
		
		//get the domains the current user has access to
		Set<String> usersDomains = getDomains();
		
		if (sample.getDomain() == null || sample.getDomain().length() == 0) {
			//if the sample doesn't have a domain, and the user has one domain, then they must be submitting to that domain
			if (usersDomains.size() == 1) {
				sample = Sample.build(sample.getName(), sample.getAccession(), 
						usersDomains.iterator().next(), sample.getRelease(), sample.getUpdate(), 
						sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());
			} else {			
				//if the sample doesn't have a domain, and we can't guess one, then end
				throw new SampleDomainMissingException();
			}
		}

		//check sample is assigned to a domain that the authenticated user has access to
		if (!usersDomains.contains(sample.getDomain())) {
			log.info("User asked to submit to domain "+sample.getDomain()+" but has access to "+usersDomains);
			throw new SampleNotAccessibleException();
		}
		
		return sample;
	}
}