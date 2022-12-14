package com.bootcamp.bank.service.pasive;

import ch.qos.logback.classic.Logger;
import com.bootcamp.bank.dto.AccountDto;
import com.bootcamp.bank.dto.DepositMoneyDTO;
import com.bootcamp.bank.dto.WithDrawMoneyDTO;
import com.bootcamp.bank.model.account.pasive.SavingAccount;
import com.bootcamp.bank.model.generic.Movements;
import com.bootcamp.bank.repository.account.pasive.SavingAccountRepository;
import com.bootcamp.bank.repository.generic.MovementsRepository;
import com.bootcamp.bank.utils.Constants;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

@Service
public class SavingAccountService {

    @Autowired
    SavingAccountRepository savingAccountRepository;

    @Autowired
    MovementsRepository movementsRepository;

    private static final Logger logger
            = (Logger) LoggerFactory.getLogger(SavingAccountService.class);

    public Flux<SavingAccount> list(){
        return savingAccountRepository.findAll();
    }

    public Mono<SavingAccount> findByCustomer(String id){
        return savingAccountRepository.getByIdCustomer(id);
    }

    public Mono<SavingAccount> savePersonalSavingAccount(AccountDto accountDto){

        SavingAccount savingAccount = new SavingAccount();
        logger.info("SAVE : savePersonalSavingAccount()");
        if(accountDto.getType().equalsIgnoreCase(Constants.PERSONAL)){
            Mono<SavingAccount> savingAccountFind = savingAccountRepository.getByIdCustomer(accountDto.getIdCustomer());

            return savingAccountFind.flux().count().flatMap(l -> (l < 1)
                            ? Mono.just(savingAccount).flatMap( s -> {
                        s.setCode(accountDto.getCode());
                        s.setAmount(accountDto.getAmount());
                        s.setTypeCustomer(Constants.BUSINESS);
                        s.setIdCustomer(accountDto.getIdCustomer());
                        s.setTransaction(0);
                        return savingAccountRepository.save(s).flatMap( savingAccount1 -> {
                            Movements movement = new Movements();
                            movement.setType(Constants.MOV_ACCOUNT);
                            movement.setCreation(new Date());
                            movement.setCustomer(accountDto.getIdCustomer());
                            movement.setTable(savingAccount1.getId());
                            movement.setStatus(1);
                            return movementsRepository.save(movement).flatMap( movement1 -> Mono.just(savingAccount1));
                        });
                    })
                            : Mono.error(new IllegalArgumentException("Personal clients can have only one current account"))
            );
        }
        return Mono.error(new IllegalArgumentException("Only Personal Customer can have an saving account !!"));
    }

    public Mono<Boolean> delete(String id){
        return savingAccountRepository.findById(id)
                .flatMap(ca -> savingAccountRepository.delete(ca)
                        .then(Mono.just(Boolean.TRUE)))
                .defaultIfEmpty(Boolean.FALSE);
    }

    public Mono<SavingAccount> update(SavingAccount savingAccount){
        return savingAccountRepository.getSavingAccountById(savingAccount.getId()).flatMap( ba -> {
            return savingAccountRepository.save(savingAccount);
        });
    }

    public Mono<String> depositMoneysavingAccount(DepositMoneyDTO depositMoneyDTO){
        return savingAccountRepository.getSavingAccountByCode(depositMoneyDTO.getCodeAccount())
                .flatMap(ba -> {
                    if(ba.getType().equalsIgnoreCase(Constants.SAVING_ACCOUNT)){
                        Float currentAmount = ba.getAmount();
                        Integer transaction = ba.getTransaction();
                        Float newAmount = 0F;
                        if(transaction <= Constants.SAVING_MAX_TRANSACTION){
                            newAmount = depositMoneyDTO.getAmount() + currentAmount;
                            ba.setAmount(newAmount);
                            ba.setTransaction(transaction+1);
                        }else{
                            newAmount = depositMoneyDTO.getAmount() + currentAmount - Constants.COMMISION_TRANSACTION ;
                            ba.setAmount(newAmount);
                            ba.setTransaction(transaction+1);
                        }
                        if(newAmount < 0F){
                            return Mono.error(new IllegalArgumentException("There is not enough money in the account !"));
                        }
                        return this.update(ba).flatMap( businessAccount1  -> {
                            Movements movement = new Movements();
                            movement.setType(Constants.MOV_DEPOSIT_MONEY);
                            movement.setCreation(new Date());
                            // movement.setCustomer(depositMoneyDTO.getIdCustomer());
                            movement.setTable(businessAccount1.getId());
                            movement.setStatus(1);
                            movement.setDescription(depositMoneyDTO.getCodeAccount());
                            return movementsRepository.save(movement).flatMap( movement1 -> Mono.just("Money Update ..!! "));

                        });
                    }
                    return Mono.error(new IllegalArgumentException("It is not Saving Account "));
                });
    }


    public Mono<String> withdrawMoneysavingAccount(WithDrawMoneyDTO withDrawMoneyDTO){
        return savingAccountRepository.getSavingAccountByCode(withDrawMoneyDTO.getCodeAccount())
                .flatMap(ba -> {
                    if(ba.getType().equalsIgnoreCase(Constants.SAVING_ACCOUNT)){
                        Float currentAmount = ba.getAmount();
                        Integer transaction = ba.getTransaction();
                        Float newAmount = 0F;
                        if(transaction <= Constants.SAVING_MAX_TRANSACTION){
                            newAmount = currentAmount - withDrawMoneyDTO.getAmount() ;
                            ba.setAmount(newAmount);
                            ba.setTransaction(transaction+1);
                        }else{
                            newAmount = currentAmount + withDrawMoneyDTO.getAmount() - Constants.COMMISION_TRANSACTION;
                            ba.setAmount(newAmount);
                            ba.setTransaction(transaction+1);
                        }
                        if(newAmount < 0F){
                            return Mono.error(new IllegalArgumentException("There is not enough money in the account !"));
                        }
                        return this.update(ba).flatMap( businessAccount1  -> {
                            Movements movement = new Movements();
                            movement.setType(Constants.MOV_WITHDRAW_MONEY);
                            movement.setCreation(new Date());
                            // movement.setCustomer(depositMoneyDTO.getIdCustomer());
                            movement.setTable(businessAccount1.getId());
                            movement.setStatus(1);
                            movement.setDescription(withDrawMoneyDTO.getCodeAccount());
                            return movementsRepository.save(movement).flatMap( movement1 -> Mono.just("Money Update ..!! "));
                        });
                    }
                    return Mono.error(new IllegalArgumentException("It is not Saving Account "));
                });
    }

    public Mono<String> getMoneyAvailable(String code_account){
        return savingAccountRepository.getSavingAccountByCode(code_account)
                .flatMap( ba -> {
                    return Mono.just(ba.getAmount().toString());
                }).switchIfEmpty(Mono.error(new IllegalArgumentException("Account not found !! ")));
    }


}
