/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.europeana.corelib.solr.utils.construct;

import eu.europeana.corelib.definitions.model.EdmLabel;
import eu.europeana.corelib.definitions.solr.entity.Aggregation;
import eu.europeana.corelib.definitions.solr.entity.WebResource;
import eu.europeana.corelib.solr.utils.SolrUtils;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author Yorgos.Mamakis@ europeana.eu
 */
public class AggregationSolrCreator {

    public void create(SolrInputDocument doc, Aggregation aggr) {
        SolrUtils.addFromString(doc, EdmLabel.PROVIDER_AGGREGATION_ORE_AGGREGATION, aggr.getAbout());
        SolrUtils.addFromMap(doc, EdmLabel.PROVIDER_AGGREGATION_DC_RIGHTS, aggr.getDcRights());
        SolrUtils.addFromMap(doc, EdmLabel.PROVIDER_AGGREGATION_EDM_RIGHTS, aggr.getEdmRights());
        SolrUtils.addFromMap(doc, EdmLabel.PROVIDER_AGGREGATION_EDM_DATA_PROVIDER, aggr.getEdmDataProvider());
        SolrUtils.addFromMap(doc, EdmLabel.PROVIDER_AGGREGATION_EDM_PROVIDER, aggr.getEdmProvider());
        SolrUtils.addFromStringArray(doc, EdmLabel.PROVIDER_AGGREGATION_ORE_AGGREGATES, aggr.getAggregates());
        SolrUtils.addFromStringArray(doc, EdmLabel.PROVIDER_AGGREGATION_EDM_HASVIEW, aggr.getHasView());
        SolrUtils.addFromString(doc, EdmLabel.PROVIDER_AGGREGATION_EDM_AGGREGATED_CHO, aggr.getAggregatedCHO());
        SolrUtils.addFromString(doc, EdmLabel.PROVIDER_AGGREGATION_EDM_IS_SHOWN_AT, aggr.getEdmIsShownAt());
        SolrUtils.addFromString(doc, EdmLabel.PROVIDER_AGGREGATION_EDM_IS_SHOWN_BY, aggr.getEdmIsShownBy());
        SolrUtils.addFromString(doc, EdmLabel.PROVIDER_AGGREGATION_EDM_OBJECT, aggr.getEdmObject());
        SolrUtils.addFromString(doc, EdmLabel.EDM_UGC, aggr.getEdmUgc());
        doc.addField(EdmLabel.PREVIEW_NO_DISTRIBUTE.toString(), aggr.getEdmPreviewNoDistribute());
        if (aggr.getWebResources() != null) {
            for (WebResource wr : aggr.getWebResources()) {
                new WebResourceSolrCreator().create(doc, wr);
            }
        }
    }
}
