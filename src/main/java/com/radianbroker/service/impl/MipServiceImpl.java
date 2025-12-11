package com.radianbroker.service.impl;

import com.radianbroker.entity.Mip;
import com.radianbroker.entity.Ris;
import com.radianbroker.repository.MipRepository;
import com.radianbroker.service.RisService;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.radianbroker.service.MipService;

@Service
public class MipServiceImpl implements MipService {
	Logger logger = LoggerFactory.getLogger(MipServiceImpl.class);

	@Value("${broker.ris.code}")
	private String risCode;

	@Autowired
	RisService risService;
	@Autowired
	MipRepository mipRepository;

	@Override
	public List<Mip> getMips(Long risId)throws Exception {
		return mipRepository.findAllByRisId(risId);
	}
}
