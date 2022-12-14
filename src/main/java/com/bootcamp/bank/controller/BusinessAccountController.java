package com.bootcamp.bank.controller;


import com.bootcamp.bank.dto.*;
import com.bootcamp.bank.model.account.active.BusinessAccount;
import com.bootcamp.bank.service.active.BusinessAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/BusinessAccount")
public class BusinessAccountController {

    @Autowired
    BusinessAccountService  businessAccountService;



    @GetMapping("/list")
    public Flux<BusinessAccount> list(){
        return businessAccountService.list();
    }

    @GetMapping("/findByCustomer/{id}")
    public Flux<BusinessAccount> findByCustomer(@PathVariable String id){
        return businessAccountService.findByCustomer(id);
    }

    @PostMapping("/savePersonal")
    public Mono<ResponseEntity<BusinessAccount>> savePersonal(@RequestBody BusinessAccountDTO businessAccountDTO){

           return businessAccountDTO.validator()
                   .map(myerror -> new ResponseEntity<>(myerror , HttpStatus.CREATED) )
                   .switchIfEmpty(
                         businessAccountService.saveBusinessAccountforPersonal(businessAccountDTO)
                          .map(businessAccount -> new ResponseEntity<>(businessAccount , HttpStatus.CREATED))
                          .defaultIfEmpty(new ResponseEntity<>(HttpStatus.BAD_REQUEST))
                    );
    }

    @PostMapping("/saveBusiness")
    public Mono<ResponseEntity<BusinessAccount>> saveBusiness(@RequestBody BusinessAccountDTO businessAccountDTO){

        return businessAccountDTO.validator()
                .map(myerror -> new ResponseEntity<>(myerror , HttpStatus.CREATED) )
                .switchIfEmpty(
                        businessAccountService.saveBusinessAccountforBusiness(businessAccountDTO)
                                .map(businessAccount -> new ResponseEntity<>(businessAccount , HttpStatus.CREATED))
                                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.BAD_REQUEST))
                );

    }

    @DeleteMapping("/delete/{id}")
    public Mono<ResponseEntity<String>> delete(@PathVariable Long id) {
        return businessAccountService.delete(id)
                .filter(deleteSavingAccount -> deleteSavingAccount)
                .map(deleteCustomer -> new ResponseEntity<>("Business-Account Deleted", HttpStatus.ACCEPTED))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @PostMapping("/depositMoney")
    public Mono<ResponseEntity<String>> depositMoney(@RequestBody DepositMoneyDTO depositMoneyDTO){
        return depositMoneyDTO.validator()
                .switchIfEmpty(
                         businessAccountService.depositMoneyBusinessAccount(depositMoneyDTO)
                                 .map(depositMoney -> new ResponseEntity<>(depositMoney , HttpStatus.CREATED))
                                 .defaultIfEmpty(new ResponseEntity<>(HttpStatus.BAD_REQUEST))
                );
    }

    @PostMapping("/withdrawMoney")
    public Mono<ResponseEntity<String>> withdrawMoney(@RequestBody WithDrawMoneyDTO withDrawMoneyDTO){
        return withDrawMoneyDTO.validator()
                .switchIfEmpty(
                        businessAccountService.withdrawMoneyBusinessAccount(withDrawMoneyDTO)
                                .map(withdrawMoney -> new ResponseEntity<>(withdrawMoney , HttpStatus.CREATED))
                                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.BAD_REQUEST))
                );
    }

    @PostMapping("/setholders")
    public Mono<ResponseEntity<String>> setholders(@RequestBody HolderDTO holderDTO){
        return holderDTO.validator()
                .switchIfEmpty(
                        businessAccountService.setHolders(holderDTO)
                                .map(withdrawMoney -> new ResponseEntity<>(withdrawMoney , HttpStatus.CREATED))
                                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.BAD_REQUEST))
                );
    }


    @PostMapping("/setsignatureAuthorized")
    public Mono<ResponseEntity<String>> setsignatureAuthorized(@RequestBody HolderDTO holderDTO){
        return holderDTO.validator()
                .switchIfEmpty(
                        businessAccountService.setHolders(holderDTO)
                                .map(withdrawMoney -> new ResponseEntity<>(withdrawMoney , HttpStatus.CREATED))
                                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.BAD_REQUEST))
                );
    }

    @GetMapping("/getMoneyAvailable/{code_account}")
    public Mono<ResponseEntity<String>> getMoneyAvailable(@PathVariable String code_account){
        return businessAccountService.getMoneyAvailable(code_account)
                .map(moneyAvailable -> new ResponseEntity<>(moneyAvailable , HttpStatus.CREATED))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }





}
