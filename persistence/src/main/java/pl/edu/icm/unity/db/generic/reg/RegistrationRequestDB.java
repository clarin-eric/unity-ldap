/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.server.registries.LocalCredentialsRegistry;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Easy access to {@link RegistrationRequestState} storage.
 * <p>
 * Note - it is more effective to implement consistency checking in the manager object,
 * and it is done there.
 * @author K. Benedyczak
 */
@Component
public class RegistrationRequestDB extends GenericObjectsDB<RegistrationRequestState>
{
	@Autowired
	public RegistrationRequestDB(RegistrationRequestHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager,
			LocalCredentialsRegistry authnRegistry, CredentialDB credentialDB)
	{
		super(handler, dbGeneric, notificationManager, RegistrationRequestState.class,
				"registration request");
		notificationManager.addListener(new RequestCredentialChangeListener(sql -> getAll(sql)));
	}
}
