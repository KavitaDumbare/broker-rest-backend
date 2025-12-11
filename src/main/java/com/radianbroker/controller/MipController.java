package com.radianbroker.controller;


import com.radianbroker.entity.Mip;
import com.radianbroker.exceptions.CustomExceptionHandler;
import com.radianbroker.service.MipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MipController {

	@Autowired
	MipService mipService;

	@GetMapping("/admin/mips")
	public ResponseEntity<?> getMips(@RequestParam Long risId) {
		try {
			return new ResponseEntity<>(mipService.getMips(risId), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(new CustomExceptionHandler(e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
		}
	}

}
