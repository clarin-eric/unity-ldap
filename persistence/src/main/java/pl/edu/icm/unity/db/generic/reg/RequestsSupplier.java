/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.UserRequestState;

public interface RequestsSupplier
{
	List<? extends UserRequestState<?>> getRequests(SqlSession sql) throws EngineException;
}