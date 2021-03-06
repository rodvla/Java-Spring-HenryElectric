package com.henry.medidor.base.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.henry.medidor.base.model.Medida;
import com.henry.medidor.base.model.Medidor;
import com.henry.medidor.base.repository.MedidorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Service
public class MedidorService {

    @Autowired
    private MedidorRepository medidorRepository;

    @Autowired
    private MedidaService medidaService;

    public List<Medidor> getMedidores() {
        return medidorRepository.findAll();
    }

    public Medidor getMedidor(Integer id) {
        return medidorRepository.findById(id).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND));
    }

    public Medidor addMedidor(Medidor medidor) {
        return medidorRepository.save(medidor);
    }

    public Medidor editMedidor(Medidor medido) {
        Medidor medidor = getMedidor(medido.getId());
        Medidor editMedidor = new Medidor();
        editMedidor.setId(medidor.getId());
        if (medido.getMarca() != null) {
            editMedidor.setMarca(medido.getMarca());
        } else {
            editMedidor.setMarca(medidor.getMarca());
        }
        if (medido.getModelo() != null) {
            editMedidor.setModelo(medido.getModelo());
        } else {
            editMedidor.setModelo(medidor.getModelo());
        }
        if (medido.getMedidas() != null) {
            editMedidor.setMedidas(medido.getMedidas());
        } else {
            editMedidor.setMedidas(medidor.getMedidas());
        }
        return medidorRepository.save(editMedidor);
    }

    public void deleteMedidorByid(Integer id) {
        medidorRepository.deleteById(id);
    }

    public void addMedidaToMedidor(Integer idMedidor, Medida medida) {
        Medidor medidorR = getMedidor(idMedidor);
        Medida medidaAdd = medidaService.addMedida(medida);
        List<Medida> lista = medidorR.getMedidas();
        lista.add(medidaAdd);
        medidorRepository.save(medidorR);
    }

    @Scheduled(fixedRate = 50)
    public void sendMedicion() throws IOException, InterruptedException {
        Medida medida;
        Float valor;
        Date date;
        TimeZone.setDefault( TimeZone.getTimeZone("GMT"));
        if(getMedidor(1234).getMedidas().size() == 0){
            valor = 0.0f;
            date = new Date();
            medida = new Medida(valor,date);
        }else{
            valor = getMedidor(1234).getMedidas().get(getMedidor(1234).getMedidas().size()-1).getValor() + 5.0f;
            Date date2 = getMedidor(1234).getMedidas().get(getMedidor(1234).getMedidas().size()-1).getFecha();
            Calendar c = Calendar.getInstance();
            c.setTime(date2);
            c.add(Calendar.HOUR, 12);
            date= c.getTime();
            medida = new Medida(valor, date);
        }

        HttpClient client = HttpClient.newBuilder().build();

        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(medida);

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8082/medidor/1234/medida"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.discarding());

        Medida addMedida = medidaService.addMedida(medida);
        Medidor medidor = getMedidor(1234);
        medidor.getMedidas().add(medida);
        medidorRepository.save(medidor);


        System.out.println("medida enviada: " + addMedida);

    }

}
