package com.radianbroker.service;

import java.util.List;

import com.radianbroker.entity.Mip;



public interface MipService {

	List<Mip> getMips(Long risId)throws Exception;

}
