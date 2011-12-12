use specify_generic;
CREATE TABLE `spauditlog` (
  `SpAuditLogID` int(11) NOT NULL AUTO_INCREMENT,
  `TimestampCreated` datetime NOT NULL,
  `TimestampModified` datetime DEFAULT NULL,
  `Version` int(11) DEFAULT NULL,
  `Action` tinyint(4) NOT NULL,
  `ParentRecordId` int(11) DEFAULT NULL,
  `ParentTableNum` smallint(6) DEFAULT NULL,
  `RecordId` int(11) DEFAULT NULL,
  `RecordVersion` int(11) NOT NULL,
  `TableNum` smallint(6) NOT NULL,
  `CreatedByAgentID` int(11) DEFAULT NULL,
  `ModifiedByAgentID` int(11) DEFAULT NULL,
  PRIMARY KEY (`SpAuditLogID`),
  KEY `FKD51C16E67699B003` (`CreatedByAgentID`),
  KEY `FKD51C16E65327F942` (`ModifiedByAgentID`),
  CONSTRAINT `FKD51C16E65327F942` FOREIGN KEY (`ModifiedByAgentID`) REFERENCES `agent` (`AgentID`),
  CONSTRAINT `FKD51C16E67699B003` FOREIGN KEY (`CreatedByAgentID`) REFERENCES `agent` (`AgentID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8