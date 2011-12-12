use specify_generic;
insert into taxon (`timestampcreated`,`version`,`name`,`fullname`,`taxontreedefid`,`taxontreedefitemid`,`parentid`) values (now(),1,'1','1',1,1,null);
set @parentId = last_insert_id();
insert into taxon (`timestampcreated`,`version`,`name`,`fullname`,`taxontreedefid`,`taxontreedefitemid`,`parentid`) values (now(),1,'1','1.1',1,1,@parentId);
set @11 = last_insert_id();
insert into taxon (`timestampcreated`,`version`,`name`,`fullname`,`taxontreedefid`,`taxontreedefitemid`,`parentid`) values (now(),1,'1','1.1.1',1,1,@11);
insert into taxon (`timestampcreated`,`version`,`name`,`fullname`,`taxontreedefid`,`taxontreedefitemid`,`parentid`) values (now(),1,'2','1.1.2',1,1,@11);
insert into taxon (`timestampcreated`,`version`,`name`,`fullname`,`taxontreedefid`,`taxontreedefitemid`,`parentid`) values (now(),1,'2','1.2',1,1,@parentId);
