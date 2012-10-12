/*
 * Copyright 2007-2012 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 * 
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */
package eu.europeana.corelib.solr.service.impl;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.util.NamedList;
import org.springframework.beans.factory.annotation.Value;

import eu.europeana.corelib.definitions.exception.ProblemType;
import eu.europeana.corelib.definitions.solr.Facet;
import eu.europeana.corelib.definitions.solr.QueryType;
import eu.europeana.corelib.definitions.solr.beans.BriefBean;
import eu.europeana.corelib.definitions.solr.beans.FullBean;
import eu.europeana.corelib.definitions.solr.beans.IdBean;
import eu.europeana.corelib.definitions.solr.model.Query;
import eu.europeana.corelib.definitions.solr.model.Term;
import eu.europeana.corelib.solr.bean.impl.ApiBeanImpl;
import eu.europeana.corelib.solr.bean.impl.BriefBeanImpl;
import eu.europeana.corelib.solr.bean.impl.IdBeanImpl;
import eu.europeana.corelib.solr.exceptions.SolrTypeException;
import eu.europeana.corelib.solr.model.ResultSet;
import eu.europeana.corelib.solr.server.EdmMongoServer;
import eu.europeana.corelib.solr.server.impl.EuropeanaHttpClient;

import eu.europeana.corelib.solr.service.SearchService;
import eu.europeana.corelib.solr.service.query.MoreLikeThis;
import eu.europeana.corelib.solr.utils.SolrUtils;
import eu.europeana.corelib.tools.utils.EuropeanaUriUtils;

/**
 * @see eu.europeana.corelib.solr.service.SearchService
 * 
 * @author Yorgos.Mamakis@ kb.nl
 * 
 */
public class SearchServiceImpl implements SearchService {

	/**
	 * Default number of documents retrieved by MoreLikeThis
	 */
	private static final int DEFAULT_MLT_COUNT = 10;

	private static final String UNION_FACETS_FORMAT = "'{'!ex={0}'}'{0}";

	/**
	 * The list of possible field input for spelling suggestions
	 */
	private static final List<String> SPELL_FIELDS = Arrays.asList("who",
			"what", "where", "when", "title");

	// provided by setter
	private SolrServer solrServer;

	private SolrServer suggestionSolrServer;

	@Resource(name = "corelib_solr_mongoServer")
	private EdmMongoServer mongoServer;

	@Resource(name = "corelib_solr_httpClient")
	private EuropeanaHttpClient httpClient;

	@Resource(name = "corelib_solr_suggestionClient")
	private EuropeanaHttpClient suggestionClient;
	
	@Value("#{europeanaProperties['solr.facetLimit']}")
	private int facetLimit;

	private static final Logger log = Logger.getLogger(SearchServiceImpl.class.getName());

	// private static final String TERMS_QUERY_TYPE = "/terms";

	// private static final String TERMS_REGEX_FLAG = "case_insensitive";

	@Override
	public FullBean findById(String collectionId, String recordId)
			throws SolrTypeException {
		return findById(EuropeanaUriUtils.createEuropeanaId(collectionId,
				recordId));
	}

	@Override
	public FullBean findById(String europeanaObjectId) throws SolrTypeException {

		FullBean fullBean = mongoServer.getFullBean(europeanaObjectId);
		if (fullBean != null) {
			try {
				fullBean.setSimilarItems(findMoreLikeThis(europeanaObjectId));
			} catch (SolrServerException e) {
				log.severe("SolrServerException: " + e.getMessage());
			}
		}

		return fullBean;
	}

	@Override
	public FullBean resolve(String collectionId, String recordId)
			throws SolrTypeException {
		return resolve(EuropeanaUriUtils.createEuropeanaId(collectionId, recordId));
	}

	@Override
	public FullBean resolve(String europeanaObjectId) throws SolrTypeException {

		FullBean fullBean = mongoServer.resolve(europeanaObjectId);
		if (fullBean != null) {
			try {
				fullBean.setSimilarItems(findMoreLikeThis(europeanaObjectId));
			} catch (SolrServerException e) {
				log.severe("SolrServerException: " + e.getMessage());
			}
		}

		return fullBean;
	}

	@Override
	public List<BriefBean> findMoreLikeThis(String europeanaObjectId)
			throws SolrServerException {
		return findMoreLikeThis(europeanaObjectId, DEFAULT_MLT_COUNT);
	}

	public List<BriefBean> findMoreLikeThis(String europeanaObjectId, int count)
			throws SolrServerException {
		String query = "europeana_id:\"" + europeanaObjectId + "\"";

		SolrQuery solrQuery = new SolrQuery().setQuery(query);
		solrQuery.setQueryType(QueryType.ADVANCED.toString());
		solrQuery.set("mlt", true);
		List<String> fields = new ArrayList<String>();
		for (MoreLikeThis mltField : MoreLikeThis.values()) {
			fields.add(mltField.toString());
		}
		solrQuery.set("mlt.fl", StringUtils.join(fields, ","));
		solrQuery.set("mlt.mintf", 1);
		solrQuery.set("mlt.match.include", "false");
		solrQuery.set("mlt.count", count);

		log.info(solrQuery.toString());

		QueryResponse response = solrServer.query(solrQuery);
		log.info("elapsed time (MoreLikeThis): " + response.getElapsedTime());

		@SuppressWarnings("unchecked")
		NamedList<Object> moreLikeThisList = (NamedList<Object>) response
				.getResponse().get("moreLikeThis");
		@SuppressWarnings("unchecked")
		List<SolrDocument> docs = (List<SolrDocument>) moreLikeThisList
				.getVal(0);
		List<BriefBean> beans = new ArrayList<BriefBean>();
		for (SolrDocument doc : docs) {
			beans.add(solrServer.getBinder().getBean(BriefBeanImpl.class, doc));
		}
		return beans;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IdBean> ResultSet<T> search(Class<T> beanInterface,
			Query query) throws SolrTypeException {

		ResultSet<T> resultSet = new ResultSet<T>();
		Class<? extends IdBeanImpl> beanClazz = SolrUtils
				.getImplementationClass(beanInterface);

		if (beanClazz == BriefBeanImpl.class || beanClazz == ApiBeanImpl.class) {
			String[] refinements = query.getRefinements(true);
			if (SolrUtils.checkTypeFacet(refinements)) {
				SolrQuery solrQuery = new SolrQuery()
						.setQuery(query.getQuery());
				solrQuery.setFacet(true);
				for (Facet facet : query.getFacets()) {
					String facetToAdd = facet.toString();
					if (query.isProduceFacetUnion()) {
						facetToAdd = MessageFormat.format(UNION_FACETS_FORMAT,
								facetToAdd);
					}
					solrQuery.addFacetField(facetToAdd);
				}
				if (refinements != null) {
					solrQuery.addFilterQuery(refinements);
				}
				solrQuery.setFacetLimit(facetLimit);
				solrQuery.setRows(query.getPageSize());
				solrQuery.setStart(query.getStart());

				// These are going to change when we import ASSETS as well
				solrQuery.setQueryType(QueryType.ADVANCED.toString());
				query.setQueryType(solrQuery.getQueryType());

				solrQuery.setSortField("COMPLETENESS", ORDER.desc);
				solrQuery.setSortField("score", ORDER.desc);

				// enable spellcheck
				if (solrQuery.getStart() == null
						|| solrQuery.getStart().intValue() <= 1) {
					solrQuery.setParam("spellcheck", "on");
					solrQuery.setParam("spellcheck.collate", "true");
					solrQuery.setParam("spellcheck.extendedResults", "true");
					solrQuery.setParam("spellcheck.onlyMorePopular", "true");
					solrQuery.setParam("spellcheck.q", query.getQuery());
				}

				// add extra parameters if any
				if (query.getParameters() != null) {
					Map<String, String> parameters = query.getParameters();
					for (String key : parameters.keySet()) {
						solrQuery.setParam(key, parameters.get(key));
					}
				}

				try {
					log.info("Solr query is: " + solrQuery);
					QueryResponse queryResponse = solrServer.query(solrQuery);
					log.info("elapsed time (search): "
							+ queryResponse.getElapsedTime());

					resultSet.setResults((List<T>) queryResponse
							.getBeans(beanClazz));
					resultSet.setFacetFields(queryResponse.getFacetFields());
					resultSet.setResultSize(queryResponse.getResults()
							.getNumFound());
					resultSet.setSearchTime(queryResponse.getElapsedTime());
					resultSet.setSpellcheck(queryResponse
							.getSpellCheckResponse());
				} catch (SolrServerException e) {
					log.severe("SolrServerException: " + e.getMessage());
					resultSet = null;
					throw new SolrTypeException(e, ProblemType.MALFORMED_QUERY);
				}

			} else {
				resultSet = null;
				throw new SolrTypeException(ProblemType.INVALIDARGUMENTS);
			}

		} else {
			resultSet = null;
			throw new SolrTypeException(ProblemType.INVALIDARGUMENTS);
		}
		return resultSet;
	}

//	@Override
//	public List<Term> suggestions(String query,
//			int pageSize, String what) throws SolrTypeException {
//		SolrQuery solrQuery = new SolrQuery();
//		solrQuery.setQueryType(TERMS_QUERY_TYPE);
//		solrQuery.setTerms(true);
//		solrQuery.setTermsLimit(pageSize);
//		solrQuery.setTermsPrefix(query);
//		solrQuery.setTermsRegexFlag(TERMS_REGEX_FLAG);
//		solrQuery.addTermsField("titleSpell");
//		solrQuery.addTermsField("whoSpell");
//		solrQuery.addTermsField("whatSpell");
//		solrQuery.addTermsField("whenSpell");
//		solrQuery.addTermsField("whereSpell");
//		try {
//			QueryResponse queryResponse = solrServer.query(solrQuery);
//			TermsResponse response = queryResponse.getTermsResponse();
//			List<Term> results = new ArrayList<Term>();
//			for (TermsResponse.Term term : response.getTerms("titleSpell")) {
//				results.add(new Term(term
//						.getTerm(), term.getFrequency(), "Title"));
//			}
//			for (TermsResponse.Term term : response.getTerms("whoSpell")) {
//				results.add(new Term(term
//						.getTerm(), term.getFrequency(), "Person"));
//			}
//			for (TermsResponse.Term term : response.getTerms("whenSpell")) {
//				results.add(new Term(term
//						.getTerm(), term.getFrequency(), "Date"));
//			}
//			for (TermsResponse.Term term : response.getTerms("whatSpell")) {
//				results.add(new Term(term
//						.getTerm(), term.getFrequency(), "Subject"));
//			}
//			for (TermsResponse.Term term : response.getTerms("whereSpell")) {
//				results.add(new Term(term
//						.getTerm(), term.getFrequency(), "Place"));
//			}
//			return results;
//		} catch (SolrServerException e) {
//
//			throw new SolrTypeException(e, ProblemType.MALFORMED_QUERY);
//
//		}
//
//	}

	public List<Term> suggestions(String query, int pageSize)
			throws SolrTypeException {
		return suggestions(query, pageSize, null);
	}

	public Map<String, Map<String, Integer>> seeAlso(
			Map<String, List<String>> fields) {
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("*:*");
		solrQuery.setRows(0);
		solrQuery.setFacet(true);
		for (Entry<String, List<String>> entry : fields.entrySet()) {
			for (String value : entry.getValue()) {
				solrQuery.addFacetQuery(String.format("%s:\"%s\"",
						entry.getKey(), value));
			}
		}
		QueryResponse response;
		Map<String, Map<String, Integer>> seeAlso = new HashMap<String, Map<String, Integer>>();
		try {
			response = solrServer.query(solrQuery);
			log.info(String.format("elapsed time (seeAlso/%d): %d", response.getFacetQuery().size(), response.getElapsedTime()));
			Map<String, Integer> queries = response.getFacetQuery();
			for (Entry<String, Integer> entry : queries.entrySet()) {
				String query = entry.getKey();
				Integer count = entry.getValue();
				String[] parts = query.split(":", 2);
				String fieldName = parts[0];
				String valueValue = parts[1]
						.replaceAll("^\"", "")
						.replaceAll("\"$", "");
				Map<String, Integer> fieldSuggestions;
				if (seeAlso.containsKey(fieldName)) {
					fieldSuggestions = seeAlso.get(fieldName);
				} else {
					fieldSuggestions = new HashMap<String, Integer>();
					seeAlso.put(fieldName, fieldSuggestions);
				}
				fieldSuggestions.put(valueValue, count);
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return seeAlso;
	}

	/**
	 * Adds suggestions regarding a given query string
	 */
	public List<Term> suggestions(String query, int pageSize, String field)
			throws SolrTypeException {
		log.info(String.format("%s, %d, %s", query, pageSize, field));
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setFacet(true);
		solrQuery.setFacetMinCount(1);
		solrQuery.setFacetPrefix(query.toLowerCase());
		solrQuery.setFacetLimit(pageSize);
		solrQuery.setQuery("*:*");
		solrQuery.setRows(0);
		if (StringUtils.isBlank(field) || !SPELL_FIELDS.contains(field)) {
			solrQuery.addFacetField("whoSpell", "whatSpell", "whereSpell",
					"whenSpell", "titleSpell");
		} else if (field.equals("who")) {
			solrQuery.addFacetField("whoSpell");
		} else if (field.equals("what")) {
			solrQuery.addFacetField("whatSpell");
		} else if (field.equals("where")) {
			solrQuery.addFacetField("whereSpell");
		} else if (field.equals("when")) {
			solrQuery.addFacetField("whenSpell");
		} else if (field.equals("title")) {
			solrQuery.addFacetField("titleSpell");
		}
		List<Term> results = new ArrayList<Term>();

		QueryResponse response;
		try {
			log.info("SolrQuery: " + solrQuery);
			response = suggestionSolrServer.query(solrQuery);
			log.info("elapsed time (suggestions): " + response.getElapsedTime());

			FacetField who = response.getFacetField("whoSpell");
			if (who != null) {
				List<Count> whoSuggestions = who.getValues();
				if (whoSuggestions != null) {
					for (Count whoSuggestion : whoSuggestions) {
						results.add(new Term(whoSuggestion.getName(),
								whoSuggestion.getCount(), "Creator")); // Creator_t
					}
				}
			}

			FacetField what = response.getFacetField("whatSpell");
			if (what != null) {
				List<Count> whatSuggestions = what.getValues();
				if (whatSuggestions != null) {
					for (Count whatSuggestion : whatSuggestions) {
						results.add(new Term(whatSuggestion.getName(),
								whatSuggestion.getCount(), "Subject")); // Subject_t
					}
				}
			}

			FacetField when = response.getFacetField("whenSpell");
			if (when != null) {
				List<Count> whenSuggestions = when.getValues();
				if (whenSuggestions != null) {
					for (Count whenSuggestion : whenSuggestions) {
						results.add(new Term(whenSuggestion.getName(),
								whenSuggestion.getCount(), "Time/Period"));
					}
				}
			}

			FacetField where = response.getFacetField("whereSpell");
			if (where != null) {
				List<Count> whereSuggestions = where.getValues();
				if (whereSuggestions != null) {
					for (Count whereSuggestion : whereSuggestions) {
						results.add(new Term(whereSuggestion.getName(),
								whereSuggestion.getCount(), "Place"));
					}
				}
			}

			FacetField title = response.getFacetField("titleSpell");
			if (title != null) {
				List<Count> titleSuggestions = title.getValues();
				if (titleSuggestions != null) {
					for (Count titleSuggestion : titleSuggestions) {
						results.add(new Term(titleSuggestion.getName(),
								titleSuggestion.getCount(), "Title")); // Title_t
					}
				}
			}
		} catch (SolrServerException e) {
			log.severe("SolrServerException: " + e.getMessage());
		}
		Collections.sort(results);
		return results.size() > pageSize ? results.subList(0, pageSize)
				: results;
	}

	public void setSolrServer(SolrServer solrServer) {
		// If it is instance of CommonsHTTPSolrServer
		this.solrServer = setServer(solrServer,httpClient);
	}
	
	public void setSuggestionSolrServer(SolrServer solrServer){
		this.suggestionSolrServer = setServer(solrServer,suggestionClient);
	}
	
	private SolrServer setServer(SolrServer solrServer, EuropeanaHttpClient httpClient){
		if (solrServer instanceof CommonsHttpSolrServer) {
			try {
				// Create a new solrServer. HttpClient is final and provided
				// upon construction
				return new CommonsHttpSolrServer(
						((CommonsHttpSolrServer) solrServer).getBaseURL(),
						httpClient);
			} catch (MalformedURLException e) {
				log.severe("MalformedURLException: " + e.getMessage());
			}
		} else {
			return solrServer;
		}
		return null;
	}
}
