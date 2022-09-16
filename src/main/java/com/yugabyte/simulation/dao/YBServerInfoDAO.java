package com.yugabyte.simulation.dao;

import java.util.List;

import com.yugabyte.simulation.model.YBServerModel;

public interface YBServerInfoDAO {
    List<YBServerModel> getAll();
}
