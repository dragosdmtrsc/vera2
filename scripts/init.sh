#!/bin/bash 
 
M_ID=`nova --insecure list | grep 'Access' | awk -F "[ \t\f\n\r\v]*|[ \t\f\n\r\v]*" '{print $2}'`
IP_ADDR=`nova interface-list $M_ID | grep 10.9 | awk -F "[ \t\f\n\r\v]*|" '{print $8}'`

#mycmd=`base64 -w0 remote.sh`
#ssh -i $1 -l student $IP_ADDR "echo $mycmd | base64 -d | sudo bash"


#prepare for experiment #1 - 16, 17, 
declare -A sn_map
declare -A n_map

if [ ! -f sns.txt ]
then
    neutron subnet-list -c id -c network_id -c name -c cidr > sns.txt
fi

for i in `seq 16 23`
do
    sn_map[$i]=`cat sns.txt | grep 172.$i | awk '{print $2}'`
    n_map[$i]=`cat sns.txt | grep 172.$i | awk '{print $4}'`
done

if [ ! -f novalist.txt ]
then
    nova --insecure list > novalist.txt
fi
    
M17_ID=`cat novalist.txt | grep 'Symnet17' | awk -F "[ \t\f\n\r\v]*|[ \t\f\n\r\v]*" '{print $2}'`
M18_ID=`cat novalist.txt | grep 'Symnet18' | awk -F "[ \t\f\n\r\v]*|[ \t\f\n\r\v]*" '{print $2}'`
M19_ID=`cat novalist.txt | grep 'Symnet19' | awk -F "[ \t\f\n\r\v]*|[ \t\f\n\r\v]*" '{print $2}'`
#M17_IP_ADDR=`nova interface-list $M17_ID | grep 172. | awk -F "[ \t\f\n\r\v]*|" '{print $8}'`
#M18_IP_ADDR=`nova interface-list $M18_ID | grep 172. | awk -F "[ \t\f\n\r\v]*|" '{print $8}'`
#M19_IP_ADDR=`nova interface-list $M19_ID | grep 172. | awk -F "[ \t\f\n\r\v]*|" '{print $8}'`
echo > stats.txt

#Symnet17 = #1
#Symnet18 = #2
#Symnet19 = #3


function attach_to_subnet {
    m_id=$1
    sn_id=$2
    n_id=$3
    m_port=`nova interface-list $m_id | grep 172. | awk -F "[ \t\f\n\r\v]*|" '{print $4}'`
    nova interface-detach $m_id $m_port
    #echo "nova interface-detach $?"
    NEW_PORT=`neutron port-create --fixed_ip subnet_id=$sn_id $n_id | egrep '\sid\s' | awk -F "[ \t\f\n\r\v]*|" '{print $4}'`
    nova interface-attach $m_id --port-id $NEW_PORT
    #echo "nova interface attach $?"
}

function wait_for_host {
    
    ssh -o StrictHostKeyChecking=no -i $1 -l student $2 "ping -c 1 $3"
    resp=$?
    retries=0
    while [[ $resp -ne 0 && $retries -lt $4 ]]
    do
        ssh -o StrictHostKeyChecking=no -i $1 -l student $2 "ping -c 1 $3"
        resp=$?
        retries=$((retries+1))
    done
    
}



attach_to_subnet $M17_ID ${sn_map[19]} ${n_map[19]}
attach_to_subnet $M18_ID ${sn_map[17]} ${n_map[17]}
attach_to_subnet $M19_ID ${sn_map[18]} ${n_map[18]}
nova reboot --hard --poll $M17_ID
nova reboot --hard --poll $M19_ID
nova reboot --hard --poll $M18_ID
#a=$1
#scp -i $a $a* student@$IP_ADDR:~/.ssh/

#for s1 in `seq 16 2 22`
#do
    # s2=$((s1+1))
    # starts3=$((s1+2))
    # attach_to_subnet $M17_ID ${sn_map[$s1]} ${n_map[$s1]}
    # nova reboot --hard --poll $M17_ID

    # attach_to_subnet $M18_ID ${sn_map[$s2]} ${n_map[$s2]}
    # nova reboot --hard --poll $M18_ID
    
    # M17_IP_ADDR=`nova interface-list $M17_ID | grep 172. | awk -F "[ \t\f\n\r\v]*|" '{print $8}'`
    # M18_IP_ADDR=`nova interface-list $M18_ID | grep 172. | awk -F "[ \t\f\n\r\v]*|" '{print $8}'`
    # #one way
    # wait_for_host $1 $IP_ADDR $M17_IP_ADDR 20
    # wait_for_host $1 $IP_ADDR $M18_IP_ADDR 20
    
    # ssh -o StrictHostKeyChecking=no -i $1 -l student $IP_ADDR "ssh -o StrictHostKeyChecking=no -i $1 -l student $M17_IP_ADDR \"ping -c 1 $M18_IP_ADDR\""
    # echo "$M17_IP_ADDR,$M18_IP_ADDR,$?" >> stats.txt
    # ssh -o StrictHostKeyChecking=no -i $1 -l student $IP_ADDR "ssh -o StrictHostKeyChecking=no -i $1 -l student $M18_IP_ADDR \"ping -c 1 $M17_IP_ADDR\""
    # echo "$M18_IP_ADDR,$M17_IP_ADDR,$?" >> stats.txt
    
    # for s3 in `seq $starts3 23`
    # do
        # echo "Now S3 = $s3"
        # attach_to_subnet $M19_ID ${sn_map[$s3]} ${n_map[$s3]}
        # nova reboot --hard --poll $M19_ID
        # M19_IP_ADDR=`nova interface-list $M19_ID | grep 172. | awk -F "[ \t\f\n\r\v]*|" '{print $8}'`
        # wait_for_host $1 $IP_ADDR $M19_IP_ADDR 20
        # #one way
        # ssh -o StrictHostKeyChecking=no -i $1 -l student $IP_ADDR "ssh -i $1 -l student $M17_IP_ADDR \"ping -c 1 $M19_IP_ADDR\""
        # echo "$M17_IP_ADDR,$M19_IP_ADDR,$?" >> stats.txt
        # ssh -o StrictHostKeyChecking=no -i $1 -l student $IP_ADDR "ssh  -o StrictHostKeyChecking=no -i $1 -l student $M18_IP_ADDR \"ping -c 1 $M19_IP_ADDR\""
        # echo "$M18_IP_ADDR,$M19_IP_ADDR,$?" >> stats.txt

        # #and viceversa
        # ssh -o StrictHostKeyChecking=no -i $1 -l student $IP_ADDR "ssh -o StrictHostKeyChecking=no -i $1 -l student $M19_IP_ADDR \"ping -c 1 $M17_IP_ADDR\""
        # echo "$M19_IP_ADDR,$M17_IP_ADDR,$?" >> stats.txt
        # ssh -o StrictHostKeyChecking=no -i $1 -l student $IP_ADDR "ssh -o StrictHostKeyChecking=no -i $1 -l student $M19_IP_ADDR \"ping -c 1 $M18_IP_ADDR\""
        # echo "$M19_IP_ADDR,$M18_IP_ADDR,$?" >> stats.txt
        

    # done
# done


#Step 2: 18, 19, 20-23
#Step 3: 20, 21, 22-23
#Step 4: 22, 23, -


