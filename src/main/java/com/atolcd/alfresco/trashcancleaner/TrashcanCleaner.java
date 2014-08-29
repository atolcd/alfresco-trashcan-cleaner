package com.atolcd.alfresco.trashcancleaner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TrashcanCleaner {
  private static Log logger = LogFactory.getLog(TrashcanCleaner.class);

  private NodeService nodeService;
  private TransactionService transactionService;
  private int protectedDays = 7;
  private StoreRef storeRef;

  /**
   * @param nodeService the nodeService to set
   */
  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  /**
   * @param transactionService the transactionService to set
   */
  public void setTransactionService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  /**
   * @param protectedDays The protectedDays to set.
   */
  public void setProtectedDays(int protectedDays) {
    this.protectedDays = protectedDays;
    if (logger.isDebugEnabled()) {
      if (this.protectedDays > 0) {
        logger.debug("Deleted items will be protected during "
            + protectedDays + " days");
      } else {
        logger.debug("Trashcan cleaner has been desactivated ('protectedDays' set to an incorrect value)");
      }
    }
  }

  public void setStoreUrl(String storeUrl) {
    this.storeRef = new StoreRef(storeUrl);
  }

  public void execute() {
    if (this.protectedDays > 0) {
      NodeRef archiveRoot = nodeService.getRootNode(storeRef);
      List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(archiveRoot);
      List<NodeRef> deletedItemsToPurge = new ArrayList<NodeRef>(childAssocs.size());
      Date toDate = new Date(
          new Date().getTime()
              - (1000L * 60L * 60L * 24L * Long
              .valueOf(this.protectedDays)));
      for (ChildAssociationRef childAssoc : childAssocs) {
        NodeRef deletedItemRef = childAssoc.getChildRef();
        Date archivedDate = (Date) nodeService.getProperty(deletedItemRef, ContentModel.PROP_ARCHIVED_DATE);
        if (archivedDate != null && archivedDate.compareTo(toDate) < 0) {
          deletedItemsToPurge.add(deletedItemRef);
        }
      }

      UserTransaction tx = null;
      ResultSet results = null;
      try {
        tx = this.transactionService
            .getNonPropagatingUserTransaction(false);
        tx.begin();

        if (logger.isInfoEnabled()) {
          int itemsToPurge = deletedItemsToPurge.size();

          if (itemsToPurge > 0) {
            logger.info("Trashcan Cleaner is about to purge " + itemsToPurge + " items.");
            logger.info("Items to purge:");
            for (NodeRef item : deletedItemsToPurge) {
              String itemName = (String) this.nodeService.getProperty(item, ContentModel.PROP_NAME);
              logger.info(" - " + itemName);
            }
          } else {
            logger.info("There's no items to purge.");
          }
        }
        for (NodeRef item : deletedItemsToPurge) {
          this.nodeService.deleteNode(item);
        }

        tx.commit();
      } catch (Throwable err) {
        if (logger.isWarnEnabled()) {
          logger.warn("Error while cleaning the trashcan: "
              + err.getMessage());
        }
        try {
          if (tx != null) {
            tx.rollback();
          }
        } catch (Exception tex) {
          if (logger.isWarnEnabled()) {
            logger.warn("Error during the rollback: "
                + tex.getMessage());
          }
        }
      } finally {
        if (results != null) {
          results.close();
        }
      }
    }
  }
}
