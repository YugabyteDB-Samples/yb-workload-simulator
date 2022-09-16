	/*+ Set(enable_hashjoin off) Set(enable_mergejoin off) Set(enable_seqscan off) IndexScan(t topology_acidx2a) IndexScan(t1 topology_acidx2) */
	WITH RECURSIVE locs AS (
		    SELECT
		        id,idtype,idname,parentid,children,depth
		    FROM
		        topology
		    WHERE
		        id = 'c9ef84a7-f50c-4e84-a039-d51292f6fd19'
		        AND idtype IN ('USER', 'LOCATION_GROUP', 'LOCATION')
		    UNION ALL
		        SELECT
		            t.id,
		            t.idtype,
		            t.idname,
		            t.parentid,
		            t.children,
		            t.depth
		        FROM
		                topology t
		        INNER JOIN locs l ON t.parentid = l.id
		        WHERE t.idtype = 'LOCATION_GROUP' AND t.parentid <> 'null'
		) SELECT
		    id,idtype,idname,parentid,children,depth
		    FROM
		    locs l
		  UNION ALL
		  SELECT t1.id, t1.idtype, t1.idname, t1.parentid, t1.children, t1.depth
		  FROM topology t1 INNER JOIN locs l ON t1.parentid = l.id WHERE t1.idtype = 'LOCATION' AND t1.parentid <> 'null';

		  
		  
insert into topo2(parentid, id, idtype, children, depth)  values ('null', '1', 'USER', 5, 3);

insert into topo2(parentid, id, idtype, children, depth)  values ('1', '2', 'LOCATION_GROUP', 3, 2);
insert into topo2(parentid, id, idtype, children, depth)  values ('1', '3', 'LOCATION_GROUP', 2, 2);

insert into topo2(parentid, id, idtype, children, depth)  values ('2', '4', 'LOCATION', 0, 1);
insert into topo2(parentid, id, idtype, children, depth)  values ('2', '5', 'LOCATION', 0, 1);
insert into topo2(parentid, id, idtype, children, depth)  values ('2', '6', 'LOCATION', 0, 1);

insert into topo2(parentid, id, idtype, children, depth)  values ('3', '7', 'LOCATION', 0, 1);
insert into topo2(parentid, id, idtype, children, depth)  values ('3', '8', 'LOCATION', 0, 1);

update topo2 set childlist = childlist ||'{2}' where id = '';
update topo2 set childlist = childlist ||'{3}' where id = '';



/*+ Set(enable_hashjoin off) Set(enable_mergejoin off) Set(enable_seqscan off) IndexScan(t topology_idx2) IndexScan(t1 topology_idx2) */	WITH RECURSIVE locs AS (
		    SELECT
		        id,idtype,idname,parentid,children,depth		    FROM
		        topology
		    WHERE
		        id = 'c50b9e92-0e35-4da8-a5cb-e59751eb4ba7'
    		        AND idtype IN ('USER', 'LOCATION_GROUP', 'LOCATION')
		    UNION ALL
		        SELECT
		            t.id,
		            t.idtype,
		            t.idname,
		            t.parentid,
		            t.children,
		            t.depth
		        FROM
		                topology t
		        INNER JOIN locs l ON t.parentid = l.id
		        WHERE t.idtype = 'LOCATION_GROUP' AND t.parentid <> 'null'
		) SELECT
		    id,idtype,idname,parentid,children,depth
		    FROM
		    locs l
		  UNION ALL
		  SELECT t1.id, t1.idtype, t1.idname, t1.parentid, t1.children, t1.depth
		  FROM topology t1 INNER JOIN locs l ON t1.parentid = l.id WHERE t1.idtype = 'LOCATION' AND t1.parentid <> 'null';
		  
		  
/* Set(enable_hashjoin off) Set(enable_mergejoin off) Set(enable_seqscan off) Set(transaction_read_only on) IndexScan(t topology_user_idx2) IndexScan(t1 topology_user_idx2) */
 	WITH RECURSIVE locs AS (
 		    SELECT
 		        id,idtype,idname,parentid,children,depth,userId
 		    FROM
 		        topology
 		    WHERE
 		        id = 'b30cc2f9-8900-4399-86cd-080cccbb0972'
 		    UNION ALL
 		        SELECT
 		            t.id,
 		            t.idtype,
 		            t.idname,
 		            t.parentid,
 		            t.children,
 		            t.depth,
                  t.userId
 		        FROM
 		                topology t
 		        INNER JOIN locs l ON t.parentid = l.id and t.userId = l.userId
 		        WHERE t.idtype = 'LOCATION_GROUP' AND t.parentid <> '00000000-0000-0000-0000-000000000000'
 		) SELECT
 		    id,idtype,idname,parentid,children,depth,userId
 		    FROM
 		    locs l
 		  UNION ALL
 		  SELECT t1.id, t1.idtype, t1.idname, t1.parentid, t1.children, t1.depth, t1.userId
 		  FROM topology t1 INNER JOIN locs l ON t1.parentid = l.id and t1.userId = l.userId WHERE t1.idtype = 'LOCATION' AND t1.parentid <> '00000000-0000-0000-0000-000000000000';;

/* Set(enable_hashjoin off) Set(enable_mergejoin off) Set(enable_seqscan off) Set(transaction_read_only on) IndexScan(t topology_user_idx2) IndexScan(t1 topology_user_idx2) */ WITH RECURSIVE locs AS ( SELECT id,idtype,idname,parentid,children,depth,userId FROM topology WHERE id = 'b30cc2f9-8900-4399-86cd-080cccbb0972'  UNION ALL SELECT t.id, t.idtype, t.idname, t.parentid, t.children, t.depth, t.userId FROM topology t INNER JOIN locs l ON t.parentid = l.id and t.userId = l.userId WHERE t.idtype = 'LOCATION_GROUP' AND t.parentid <> '00000000-0000-0000-0000-000000000000') SELECT id,idtype,idname,parentid,children,depth,userId FROM locs l UNION ALL SELECT t1.id, t1.idtype, t1.idname, t1.parentid, t1.children, t1.depth, t1.userId FROM topology t1 INNER JOIN locs l ON t1.parentid = l.id and t1.userId = l.userId WHERE t1.idtype = 'LOCATION' AND t1.parentid <> '00000000-0000-0000-0000-000000000000';;
