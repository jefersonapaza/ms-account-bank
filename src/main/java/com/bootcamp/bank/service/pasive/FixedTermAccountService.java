package com.bootcamp.bank.service.pasive;


import ch.qos.logback.classic.Logger;
import com.bootcamp.bank.dto.AccountDto;
import com.bootcamp.bank.dto.DepositMoneyDTO;
import com.bootcamp.bank.dto.WithDrawMoneyDTO;
import com.bootcamp.bank.model.account.pasive.FixedTermAccount;
import com.bootcamp.bank.model.generic.Movements;
import com.bootcamp.bank.repository.account.pasive.FixedTermAccountRepository;
import com.bootcamp.bank.repository.generic.MovementsRepository;
import com.bootcamp.bank.utils.Constants;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

@Service
public class FixedTermAccountService {

    @Autowired
    private FixedTermAccountRepository fixedTermAccountRepository;

    @Autowired
    MovementsRepository movementsRepository;

    private static final Logger logger
            = (Logger) LoggerFactory.getLogger(FixedTermAccountService.class);


    public Flux<FixedTermAccount> list(){
        return fixedTermAccountRepository.findAll();
    }

    public Mono<FixedTermAccount> findByCustomer(String id){
        return fixedTermAccountRepository.getByIdCustomer(id);
    }

    public Mono<FixedTermAccount> saveFixedTermAccount(AccountDto accountDto){
        logger.info("SAVE : saveFixedTermAccount()");
        FixedTermAccount fixedAccount = new FixedTermAccount();
        fixedAccount.setCode(accountDto.getCode());
        fixedAccount.setAmount(accountDto.getAmount());
        fixedAccount.setTypeCustomer(Constants.PERSONAL);
        fixedAccount.setTransaction(0);
        return fixedTermAccountRepository.save(fixedAccount).flatMap( fixedAccount1 -> {
            Movements movement = new Movements();
            movement.setType(Constants.MOV_ACCOUNT);
            movement.setCreation(new Date());
            movement.setCustomer(accountDto.getIdCustomer());
            movement.setTable(fixedAccount1.getId());
            movement.setStatus(1);
            return movementsRepository.save(movement).flatMap( movement1 -> Mono.just(fixedAccount1));
        });

    }

    public Mono<Boolean> delete(String id){
        return fixedTermAccountRepository.findById(id)
                .flatMap(ca -> fixedTermAccountRepository.delete(ca)
                        .then(Mono.just(Boolean.TRUE)))
                .defaultIfEmpty(Boolean.FALSE);
    }

    public Mono<FixedTermAccount> update(FixedTermAccount fixedTermAccount){
        return fixedTermAccountRepository.getFixedTermAccountById(fixedTermAccount.getId()).flatMap( ba -> {
            return fixedTermAccountRepository.save(fixedTermAccount);
        });
    }

    public Mono<String> depositMoneyFixedTermAccount(DepositMoneyDTO depositMoneyDTO){
        return fixedTermAccountRepository.getFixedTermAccountByCode(depositMoneyDTO.getCodeAccount())
                .flatMap(ba -> {
                    if(ba.getType().equalsIgnoreCase(Constants.FIXEDTERM_ACCOUNT)){
                        Float currentAmount = ba.getAmount();
                        Integer transaction = ba.getTransaction();
                        Float newAmount = 0F;
                        if(transaction <= Constants.FIXEDTERM_MAX_TRANSACTION){
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
                        return this.update(ba).flatMap( fixedTermAccount1  -> {
                            Movements movement = new Movements();
                            movement.setType(Constants.MOV_DEPOSIT_MONEY);
                            movement.setCreation(new Date());
                            // movement.setCustomer(depositMoneyDTO.getIdCustomer());
                            movement.setTable(fixedTermAccount1.getId());
                            movement.setStatus(1);
                            movement.setDescription(depositMoneyDTO.getCodeAccount());
                            return movementsRepository.save(movement).flatMap( movement1 -> Mono.just("Money Update ..!! "));

                        });
                    }
                    return Mono.error(new IllegalArgumentException("It is not Fixed-Term Account"));
                });
    }

    public Mono<String> withdrawMoneyFixedTermAccount(WithDrawMoneyDTO withDrawMoneyDTO){
        return fixedTermAccountRepository.getFixedTermAccountByCode(withDrawMoneyDTO.getCodeAccount())
                .flatMap(ba -> {
                    if(ba.getType().equalsIgnoreCase(Constants.FIXEDTERM_ACCOUNT)){
                        Float currentAmount = ba.getAmount();
                        Integer transaction = ba.getTransaction();
                        Float newAmount = 0F;
                        if(transaction <= Constants.FIXEDTERM_MAX_TRANSACTION){
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
                    return Mono.error(new IllegalArgumentException("It is not Fixed-Term Account"));
                });
    }


    public Mono<String> getMoneyAvailable(String code_account){
        return fixedTermAccountRepository.getFixedTermAccountByCode(code_account)
                .flatMap( ba -> {
                    return Mono.just(ba.getAmount().toString());
                }).switchIfEmpty(Mono.error(new IllegalArgumentException("Account not found !! ")));
    }

}
