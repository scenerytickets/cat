package com.dianping.cat.system.page.login.service;

import com.dianping.cat.system.page.login.service.LDAPAuthenticationServiceImpl;
import org.unidal.dal.jdbc.DalNotFoundException;
import org.unidal.lookup.annotation.Inject;

import com.dianping.cat.Cat;
import com.dianping.cat.home.dal.user.DpAdminLogin;
import com.dianping.cat.home.dal.user.DpAdminLoginDao;
import com.dianping.cat.home.dal.user.DpAdminLoginEntity;
import com.dianping.cat.system.page.login.spi.ISessionManager;

public class SessionManager implements ISessionManager<Session, Token, Credential> {
	@Inject
	private DpAdminLoginDao m_memberDao;

    @Inject
    private LDAPAuthenticationServiceImpl m_LDAPService;

	private Token loginByLoginName(String account, String password) {
		String base = "0000000";
		int length = account.length();
		int offset = 7 - length;

		String normalAccount = base.substring(0, offset) + account;

		try {
			DpAdminLogin member = m_memberDao.findByLoginName(normalAccount, password, DpAdminLoginEntity.READSET_FULL);
			return new Token(member.getLoginId(), member.getRealName());
		} catch (DalNotFoundException e) {
		} catch (Exception e) {
			Cat.logError(e);
		}
		return null;
	}

	private Token loginByEmail(String email, String password) {
		int index = email.indexOf("@");
		if (index < 0) {
			email = email + "@dianping.com";
		}
		try {
			DpAdminLogin member = m_memberDao.findByEmail(email, password, DpAdminLoginEntity.READSET_FULL);

			return new Token(member.getLoginId(), member.getRealName());
		} catch (DalNotFoundException e) {
		} catch (Exception e) {
			Cat.logError(e);
		}
		return null;
	}

	@Override
	public Token authenticate(Credential credential) {
		String account = credential.getAccount();
		String password = credential.getPassword();

        Token token = null;

        try {
        	token = m_LDAPService.authenticate(account, password);
        } catch (Exception e) {
        	Cat.logEvent("Login", "Login failure, uncorrected password.");
        }

        if (token != null) {
        	Cat.logEvent("Login", "Login success.");
        	return token;
        }

        Cat.logEvent("Login", "Login failure, uncorrected username.");
        return null;
	}

	@Override
	public Session validate(Token token) {

        LoginMember member = new LoginMember();

        member.setUserName(token.getUserName());
        member.setRealName(token.getRealName());

        return new Session (member);
	}
}
