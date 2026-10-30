package com.electronica.suministros.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.electronica.suministros.dto.ClienteResumenDTO;
import com.electronica.suministros.entity.Cliente;
import com.electronica.suministros.repository.ClienteRepository;
import com.electronica.suministros.repository.OrdenTrabajoRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepo;
    private final OrdenTrabajoRepository ordenRepo;

    public ClienteService(ClienteRepository clienteRepo, OrdenTrabajoRepository ordenRepo) {
        this.clienteRepo = clienteRepo;
        this.ordenRepo = ordenRepo;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepo.findAllByOrderByNombresAsc();
    }

    /** Búsqueda por nombre / razón social o por número de documento. */
    @Transactional(readOnly = true)
    public List<Cliente> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return listar();
        }
        String t = texto.trim().toLowerCase();
        return listar().stream()
                .filter(c -> c.getNombres().toLowerCase().contains(t) || c.getDniCliente().contains(t))
                .toList();
    }

    /** Clientes con OTs vigentes (JPQL con constructor DTO) para el primer combo de la consulta en cascada. */
    @Transactional(readOnly = true)
    public List<ClienteResumenDTO> resumenConOrdenes() {
        return clienteRepo.resumenConOrdenes();
    }

    /** Crea o actualiza un cliente (CRUD con validaciones de negocio). */
    @Transactional
    public void guardar(Integer idCliente, String dni, String nombres, String telefono, String correo) {
        String dniFinal = dni == null ? "" : dni.trim();
        if (!dniFinal.matches("\\d{8}|\\d{11}")) {
            throw new NegocioException("El documento debe tener 8 dígitos (DNI) o 11 dígitos (RUC)");
        }
        if (nombres == null || nombres.isBlank()) {
            throw new NegocioException("Ingrese los nombres o la razón social del cliente");
        }
        if (nombres.trim().length() > 100) {
            throw new NegocioException("El nombre no puede superar 100 caracteres");
        }
        String telefonoFinal = (telefono == null || telefono.isBlank()) ? null : telefono.trim();
        if (telefonoFinal != null && !telefonoFinal.matches("[0-9+()\\- ]{6,20}")) {
            throw new NegocioException("Ingrese un teléfono válido (solo números, espacios, + y -)");
        }
        String correoFinal = (correo == null || correo.isBlank()) ? null : correo.trim().toLowerCase();
        if (correoFinal != null && !correoFinal.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new NegocioException("Ingrese un correo válido");
        }

        Cliente c;
        if (idCliente == null) {
            if (clienteRepo.existsByDniCliente(dniFinal)) {
                throw new NegocioException("Ya existe un cliente con el documento " + dniFinal);
            }
            c = new Cliente();
        } else {
            c = clienteRepo.findById(idCliente).orElseThrow(() -> new NegocioException("El cliente no existe"));
            if (clienteRepo.existsByDniClienteAndIdClienteNot(dniFinal, idCliente)) {
                throw new NegocioException("Ya existe otro cliente con el documento " + dniFinal);
            }
        }
        c.setDniCliente(dniFinal);
        c.setNombres(nombres.trim());
        c.setTelefono(telefonoFinal);
        c.setCorreo(correoFinal);
        clienteRepo.save(c);
    }

    @Transactional
    public void eliminar(Integer idCliente) {
        if (ordenRepo.countByCliente_IdCliente(idCliente) > 0) {
            throw new NegocioException("No se puede eliminar: el cliente tiene órdenes de trabajo registradas");
        }
        clienteRepo.deleteById(idCliente);
    }
}
