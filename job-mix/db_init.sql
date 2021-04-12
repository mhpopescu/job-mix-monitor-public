-- This is for mysql database. Not used anymore

show errors;
create database nersc;
use nersc;

/*drop table stats;*/

create table stats(
    time_d      BIGINT,
    farm        char(20),
    nsim        float,
    ntrain        float,
    ndaq        float,
    nother        float,
    nall        float,
    psim        float,
    ptrain        float,
    pdaq        float,
    pother        float,
    sim_rss        float,
    train_rss    float,
    daq_rss        float,
    other_rss    float,
    all_rss        float,
    sim_vmem    float,
    train_vmem    float,
    daq_vmem    float,
    other_vmem    float,
    all_vmem    float,
    sim_eff        float,
    train_eff    float,
    daq_eff        float,
    other_eff    float,
    all_eff        float,
    speed       float default 0.0
);

drop procedure if exists add_values;
DELIMITER $$
create procedure add_values (
    in time_d   BIGINT,
    in farm     char(20),
    in nsim        float,
    in ntrain    float,
    in ndaq        float,
    in nother    float,
    in nall        float,
    in psim        float,
    in ptrain    float,
    in pdaq        float,
    in pother    float,
    in sim_rss    float,
    in train_rss    float,
    in daq_rss        float,
    in other_rss    float,
    in all_rss        float,
    in sim_vmem        float,
    in train_vmem    float,
    in daq_vmem        float,
    in other_vmem    float,
    in all_vmem        float,
    in sim_eff        float,
    in train_eff    float,
    in daq_eff        float,
    in other_eff    float,
    in all_eff        float
)
begin
    insert into stats values(time_d,farm,nsim,ntrain,ndaq,nother,nall,psim,ptrain,pdaq,pother,sim_rss,train_rss,daq_rss,other_rss,all_rss,sim_vmem,train_vmem,daq_vmem,other_vmem,all_vmem,sim_eff,train_eff,daq_eff,other_eff,all_eff,0.0);
end;
$$
DELIMITER ;

drop procedure if exists add_speed;
DELIMITER $$

/* take most recent timestamp and if speed is not set
 set it with new given value
*/
create procedure add_speed (
    in p_time     BIGINT,
    in p_speed    float,
    in p_farm     char(20)
)
begin
    declare speed_tmp float default 0;
    declare time_tmp BIGINT default 0;
    select speed, time_d into speed_tmp, time_tmp
        from stats 
        where farm = p_farm
        order by abs(time_d - p_time)
        limit 1;
    if speed_tmp < 0.00001 and time_tmp != 0
    then
        update stats set speed = p_speed
            where farm = p_farm and time_d = time_tmp;
    end if;
    set speed_tmp = 0;
    set time_tmp = 0;
end;
$$
DELIMITER ;
