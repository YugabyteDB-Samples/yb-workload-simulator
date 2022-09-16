package com.yugabyte.simulation.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.yugabyte.simulation.model.YBServerModel;

@Repository
public class YBServerInfoDAOImpl implements YBServerInfoDAO{
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public List<YBServerModel> getAll() {
        String query = "with yb_servers as (\n" +
                "       select host,port,cloud,region,zone from yb_servers()\n" +
                "   ), my_connection as (\n" +
                "      select host(inet_server_addr()) host, inet_server_port() port\n" +
                "      ,'<<- you are here' as inet_server)\n" +
                " select * from yb_servers natural left join my_connection;";
        return jdbcTemplate.query(query, new BeanPropertyRowMapper<YBServerModel>(YBServerModel.class));
    }
}
