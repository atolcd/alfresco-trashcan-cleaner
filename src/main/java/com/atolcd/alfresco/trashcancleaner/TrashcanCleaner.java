package com.atolcd.alfresco.trashcancleaner;

import java.util.Date;
import java.util.List;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.archive.NodeArchiveService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.SearchLanguageConversion;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TrashcanCleaner {
	private static Log logger = LogFactory.getLog(TrashcanCleaner.class);

	private NamespaceService namespaceService;
	private NodeArchiveService nodeArchiveService;
	private NodeService nodeService;
	private SearchService searchService;
	private TransactionService transactionService;
	private int protectedDays = 7;
	private StoreRef storeRef;

	/**
	 * @param namespaceService
	 *            The namespaceService to set.
	 */
	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}

	/**
	 * @param nodeArchiveService
	 *            The nodeArchiveService to set.
	 */
	public void setNodeArchiveService(NodeArchiveService nodeArchiveService) {
		this.nodeArchiveService = nodeArchiveService;
	}

	/**
	 * @param nodeService
	 *            the nodeService to set
	 */
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	/**
	 * @param searchService
	 *            The searchService to set.
	 */
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	/**
	 * @param transactionService
	 *            the transactionService to set
	 */
	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	/**
	 * @param protectedDays
	 *            The protectedDays to set.
	 */
	public void setProtectedDays(int protectedDays) {
		this.protectedDays = protectedDays;
		if (logger.isDebugEnabled()) {
			if (this.protectedDays > 0)
				logger.debug("Deleted items will be protected during "
						+ protectedDays + " days");
			else
				logger.debug("Trashcan cleaner has been desactivated ('protectedDays' set to an incorrect value)");
		}
	}

	public void setStoreUrl(String storeUrl) {
		this.storeRef = new StoreRef(storeUrl);
	}

	public void execute() {
		if (this.protectedDays > 0) {
			Date fromDate = new Date(0);
			Date toDate = new Date(
					new Date().getTime()
							- (1000L * 60L * 60L * 24L * Long
									.valueOf(this.protectedDays)));

			String strFromDate = ISO8601DateFormat.format(fromDate);
			String strToDate = ISO8601DateFormat.format(toDate);
			StringBuilder buf = new StringBuilder(128);
			buf.append("@")
					.append(SearchLanguageConversion
							.escapeForLucene(ContentModel.PROP_ARCHIVED_DATE
									.toPrefixString(namespaceService)))
					.append(":").append("[").append(strFromDate).append(" TO ")
					.append(strToDate).append("] ");

			String query = buf.toString();

			SearchParameters sp = new SearchParameters();
			sp.setLanguage(SearchService.LANGUAGE_LUCENE);
			sp.setQuery(query);

			NodeRef archiveRootRef = this.nodeArchiveService
					.getStoreArchiveNode(storeRef);
			sp.addStore(archiveRootRef.getStoreRef());
			if (logger.isDebugEnabled()) {
				logger.debug("Trashcan cleaner query : ");
				logger.debug(query);
			}

			UserTransaction tx = null;
			ResultSet results = null;
			try {
				tx = this.transactionService
						.getNonPropagatingUserTransaction(false);
				tx.begin();

				results = this.searchService.query(sp);
				List<NodeRef> deletedItemsToPurge = results.getNodeRefs();
				if (logger.isInfoEnabled()) {
					logger.info("Trashcan Cleaner is about to purge the following items :");
					for (NodeRef item : deletedItemsToPurge) {
						String itemName = (String) this.nodeService
								.getProperty(item, ContentModel.PROP_NAME);
						logger.info(" - " + itemName);
					}
				}
				this.nodeArchiveService.purgeArchivedNodes(deletedItemsToPurge);

				tx.commit();
			} catch (Throwable err) {
				if (logger.isWarnEnabled())
					logger.warn("Error while cleaning the trashcan : "
							+ err.getMessage());
				try {
					if (tx != null) {
						tx.rollback();
					}
				} catch (Exception tex) {
					if (logger.isWarnEnabled())
						logger.warn("Error while during the rollback : "
								+ tex.getMessage());
				}
			} finally {
				if (results != null) {
					results.close();
				}
			}
		}
	}
}
