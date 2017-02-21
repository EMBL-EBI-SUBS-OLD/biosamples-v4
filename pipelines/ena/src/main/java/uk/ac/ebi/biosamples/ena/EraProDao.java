package uk.ac.ebi.biosamples.ena;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

@Service
public class EraProDao {

	@Autowired
	@Qualifier("eraJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;
    
    private Logger log = LoggerFactory.getLogger(getClass());
	
    /**
     * Return a set of BioSamples accessions that have been updated or made public within
     * the specified date range    
     * 
     * @param minDate
     * @param maxDate
     * @return
     */
	public Set<String> getSamples(LocalDate minDate, LocalDate maxDate) {
        /*
select * from cv_status;
1       draft   The entry is draft.
2       private The entry is private.
3       cancelled       The entry has been cancelled.
4       public  The entry is public.
5       suppressed      The entry has been suppressed.
6       killed  The entry has been killed.
7       temporary_suppressed    the entry has been temporarily suppressed.
8       temporary_killed        the entry has been temporarily killed.
         */
		//once it has been public, it can only be suppressed and killed and can't go back to public again
		
		String query = "SELECT BIOSAMPLE_ID FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' "
				+ "AND STATUS_ID = 4 AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?))";
		
		Set<String> samples = new TreeSet<>();
		Date minDateOld = java.sql.Date.valueOf(minDate);
		Date maxDateOld = java.sql.Date.valueOf(maxDate);
		List<String> result = jdbcTemplate.queryForList(query, String.class, minDateOld, maxDateOld, minDateOld, maxDateOld);
		samples.addAll(result);
		return samples;
	}

	public List<String> getPrivateSamples() {
        log.info("Getting private sample ids");
        
		String query = "SELECT UNIQUE(BIOSAMPLE_ID) FROM SAMPLE WHERE STATUS_ID > 4 AND BIOSAMPLE_ID LIKE 'SAME%' "
				+ "AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' ORDER BY BIOSAMPLE_ID ASC";
        	
		List<String> sampleIds = jdbcTemplate.queryForList(query, String.class);
        
        log.info("Got "+sampleIds.size()+" private sample ids");
	
        return sampleIds;
	}
	
	public boolean getBioSamplesAuthority(String biosampleAccession) {
		String query = "SELECT BIOSAMPLE_AUTHORITY FROM SAMPLE WHERE BIOSAMPLE_ID = ? ";
		String result = jdbcTemplate.queryForObject(query, new RowMapper<String>() {

			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(1);
			}}, biosampleAccession);
		if (result.equals("Y")) { 
			return true;
		} else if (result.equals("N")) {
			return false;
		} else {
			throw new IllegalArgumentException("Unrecongized BIOSAMPLE_AUTHORITY "+result);
		}	
	}	
	
	public LocalDateTime getUpdateDateTime(String biosampleAccession) {
		String sql = "SELECT to_char(LAST_UPDATED, 'YYYY-MM-DD') FROM SAMPLE WHERE BIOSAMPLE_ID = ?";
		String dateString = jdbcTemplate.queryForObject(sql, String.class, biosampleAccession);
		log.info("Release date is "+dateString);
		return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
	}
	
	public LocalDateTime getReleaseDateTime(String biosampleAccession) {
		String sql = "SELECT to_char(FIRST_PUBLIC, 'YYYY-MM-DD') FROM SAMPLE WHERE BIOSAMPLE_ID = ?";
		String dateString = jdbcTemplate.queryForObject(sql, String.class, biosampleAccession);
		log.info("Release date is "+dateString);
		return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
	}
}