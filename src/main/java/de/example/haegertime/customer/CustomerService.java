package de.example.haegertime.customer;

import de.example.haegertime.advice.ItemAlreadyExistsException;
import de.example.haegertime.advice.ItemNotFoundException;
import de.example.haegertime.projects.Project;
import de.example.haegertime.projects.ProjectRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    private final ProjectRepository projectRepository;


    /**
     * Anlegen von neuen Kunden
     * @param customer der neue Kunde
     * @return der erzeugte Kunde
     */
    public Customer createCustomer(Customer customer) {
        boolean exists = customerRepository.existsCustomerByName(customer.getName());
        if(exists) {
            throw new ItemAlreadyExistsException("Der Name existiert bereits in der DB");
        } else {
            if (!customer.getProjects().isEmpty()) {
                for (Project project : customer.getProjects()) {
                    String projectTitle = project.getTitle();
                    boolean existsProject = projectRepository.existsProjectByTitle(projectTitle);
                    if (existsProject) {
                        throw new ItemAlreadyExistsException("Das Projekt mit dem Name " + projectTitle +
                                " existiert bereits in der DB");
                    }
                }
            }
            customerRepository.save(customer);
            return customer;
        }

    }

    /**
     * Auflisten aller Kunden in der Datenbank
     * @return Liste aller Kunden
     */
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    /**
     * Aktualisierung der Kunden
     * @param customer to update customer
     * @return updated Customer
     */
    public Customer updateCustomer(Customer customer) {
        Customer updateCustomer = customerRepository.findById(customer.getId()).orElseThrow(
                () -> new ItemNotFoundException("Diese Kunde ist nicht in der DB")
        );
        updateCustomer.setName(customer.getName());
        updateCustomer.setAddress(customer.getAddress());
        updateCustomer.setProjects(customer.getProjects());
        customerRepository.save(updateCustomer);
        return updateCustomer;
    }

    /**
     * Suchen Kunden nach ID-Nummer
     * @param id Customer ID
     * @return Customer
     */
    public Customer findById(long id) {
        return customerRepository.findById(id).orElseThrow(
                () -> new ItemNotFoundException("Diese Kunde ist nicht in der DB")
        );
    }

    /**
     * Hinzufügen ein neues Projekt zu einem Kunden
     * @param id Customer ID
     * @param project das neue Projekt
     * @return Customer
     */

    public Customer addProjectToExistingCustomer(long id, Project project) {
        Customer existingCustomer = customerRepository.findById(id).orElseThrow(
                () -> new ItemNotFoundException("Diese Kunde ist nicht in der Datenbank")
        );
        boolean existsProject = projectRepository.existsProjectByTitle(project.getTitle());
        if(!existsProject) {
            List<Project> projectList = existingCustomer.getProjects();
            projectList.add(project);
            existingCustomer.setProjects(projectList);
            customerRepository.save(existingCustomer);
            return existingCustomer;
        } else {
            throw new ItemAlreadyExistsException("Das Projekt mit Title "+project.getTitle()+
                    " ist bereits in der DB");
        }
    }

    /**
     * Löschen die Kunde mit der eingegebene ID, die zu Kunden gehörten Projekte
     * werden auch gelöscht.
     * @param id Customer ID
     */
    public void deleteCustomerById(long id) {
        if(customerRepository.findById(id).isPresent()) {
            customerRepository.deleteById(id);
        } else {
            throw new ItemNotFoundException("Diese Kunde ist nicht in der Datenbank");
        }
    }

}
