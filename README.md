U have to generate the pem key...

openssl genpkey -algorithm RSA -out keypair.pem -pkeyopt rsa_keygen_bits:2048   --------> genera el Keypair.pem

openssl rsa -pubout -in keypair.pem -out public.pem      -------> genera el public.pem

and have to paste it  in target -> key...

-----------------------------------------------------------------

Hay que generar las llave pem...

openssl genpkey -algorithm RSA -out keypair.pem -pkeyopt rsa_keygen_bits:2048   --------> genera el Keypair.pem

openssl rsa -pubout -in keypair.pem -out public.pem      -------> genera el public.pem

lo pegas en target -> key...
