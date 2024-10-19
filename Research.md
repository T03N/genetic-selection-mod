# Real World Natural selection
## Hardy-Weinberg frequency 
* https://en.wikipedia.org/wiki/Hardy%E2%80%93Weinberg_principle

The principle that states that in the absence of external selection factors, allele and genotype frequencies will remain constant over a large population size
 
* Let p and q be the frequency of an allele in a given population
* p2 + 2pq + q2 = 1

If p2 + 2pq + q2 does NOT equal to 1, evolution is occuring, and there are selection pressures happening. ie;
predators are evolving to be stronger, food scarcity, etc. The above equation can be used to “measure” evolution in a population and as a potential debugging tool if telemetry on allele frequency can be implemented.

## Genetic drift
* https://en.wikipedia.org/wiki/Genetic_drift 

Genetic drift is the change in the frequency of alleles due to chance. This Affects smaller populations the most.

## Effective population size
* https://www.uwyo.edu/dbmcd/molmark/lect07/lect7.html 

The number of a species needed to roughly match the progress of genetic drift as the wider population.

## Heritability
* https://www.britannica.com/science/heritability

The amount of observational variation found in a population that can be chalked up to genetics and not environmental differences.

* Let Vg be the variation in genotype
* Let Vp be the variation in phenotype
* Let H be the heritability estimate
* H2 = Vg / Vp

H will range from 0 to 1. If H = 0, there is no genetic variation. If H = 1, all variation is due to genetics.

## Selection differential 
* https://excellenceinbreeding.org/sites/default/files/manual/EiB_M2_Selection%20Intensity_26-10-20.pdf 

The Selection Differential is the difference between the mean trait value of the selected parents and the overall population mean.

## Response to selection
* https://www.ndsu.edu/pubweb/~mcclean/plsc431/quantgen/qgen7.htm#:~:text=The%20selection%20response%20is%20how,heriability%20by%20the%20selection%20differntial. 

Response to Selection is the "gain" made when mating selected parents. It is found by multiplying heritability by selection differential.

## Selection intensity
* https://breedplan.une.edu.au/general/a-breedplan-guide-to-genetic-improvement/#:~:text=Selection%20Intensity%20(i),(see%20Figure%201%20below). 

The Selection Intensity is the difference in the average genetic value of the animals selected for breeding versus the average genetic value of all animals in the population from which they were selected.
## Inbreeding vs Outbreeding
### Inbreeding
Inbreeding makes the offspring susceptible to recessive genetic disease from increasing the chance that those who alleles can come together. Inbreeding results in lower genetic diversity, making populations vunerable to all the same things. An example would be the Cavendish Bananas, which are all effectively identical in terms of genetics. As a result, a virus that can kill one has the potential to make the breed extinct.

### Outbreeding
Outbreeding creates genetically diverse populations, which is ideal for fitness in a species. If done too far out, and two species with low genetic compatibility breed, the offspring could have reduced fitness. An example of this would be the Mule, a horse and donkey hybrid that cannot reproduce, which does not allow for it to pass of genes to offspring.

# Basics of behavior and breeding of Minecraft animals:
## General
To breed two animals of the same species in Minecraft, both must be in "love mode," which is indicated by hearts appearing around them. Once in love mode, the animals will find a path toward each other, and an offspring will spawn. To enter love mode, animals need to be fed specific foods that vary by species. Some animals also require taming before they can breed. An animal will exit love mode after 30 seconds if it does not breed, and once they have bred, the parents cannot breed again for 5 minutes. 

This information is for the Java edition; there may be differences between the Bedrock edition and Minecraft Education. As of Minecraft version 12.1, some of the animals with the most interesting behaviors and breeding properties include horses, pandas, turtles, and foxes.

## Horse
To breed horses they first need to be tamed. This is done by repeatedly getting on the horse's back until the horse relaxes and lets the player stay on its back. To enter love mode, use a golden apple, enchanted golden apple, or golden carrot. 

Horses have health ranging from 15 to 30 hearts, where one heart equals 2 health points. Horses exhibit jump strength from 0.4 to 1.0, allowing them to jump approximately 1.11 to 5.3 blocks. Players are able to test this by jumping over walls. Their speed varies between 0.1125 to 0.3375 blocks per tick, equating to 4.74 blocks/sec to 42.16 blocks/sec. Players must use repeaters to measure this.

Specific to horses, breeding averages the traits of the two parent horses with an addition of random variance. The maximum value for strength and jump, to make the "perfect" horse is impossible to achieve through breeding. Foals inherit the color and markings of one parent, with a 13/45 chance of having a random appearance.

## Panda
Pandas can have one of 6 different personalities. Normal pandas do not have unique personality behaviors but are identifiable by their frown. Lazy pandas tend to lie on their back and are slower than normal pandas. They are also physically identifiable by their smile. Worried pandas have a worried expression on their face and they typically avoid players and hostile mobs. During thunderstorms, they are also known to hide or shake their faces. Playful pandas have a tongue that sticks out, they also roll over and jump around (usually only done by panda cubs). Aggressive pandas are identifiable by their thick eyebrows and frowns. When hit, the panda will attack the mob or player until the target dies or is no longer in the detection range. They also become aggressive towards mobs/players that hit nearby pandas. Weak pandas have teary eyes and snotty noses, and they tend to sneeze more often. Pandas have a 1/6000 chance of sneezing every tick, with weak baby pandas sneezing more frequently (1/500). Sneezing causes all adult pandas within 10 blocks to jump and may drop a slimeball. Baby pandas occasionally roll over and jump around. Brown pandas have no identifiable characteristics besides their brown fur and frown. 

For breeding, pandas need to be fed bamboo and there also must be at least one bamboo block within a 5-block radius. If there’s no bamboo block, the panda will shake their heads and will not enter love mode. Pandas have genetics and have two hidden values: a main gene and a hidden gene. Each gene corresponds to a particular personality type which is either dominant or recessive. Dominant personality types: normal, aggressive, lazy, worried, and playful. Recessive personality types: weak and brown. If the main gene is dominant, the main personality will show. If the main gene is recessive and the hidden gene is a different trait, a normal personality will show. The recessive personality type will only show if both the main and hidden genes are the same. The offspring's main and hidden genes are randomly chosen from the parent's genes. There is also a 1/32 chance for the genes to mutate. 

## Turtle
When born, turtles remember which block they were hatched in as their "home beach". Throughout its life, it will try to return to this beach. 

They require seagrass to enter love mode. Unlike other animals that will spawn offspring, turtles lay eggs. One of the turtles will gain an egg in their inventory, this will reflect physically as well. It will then return back to its home beach and lay the egg up to 9 blocks from its spawn block. The egg can only be hatched on the sand, red sand, or suspicious sand and takes about 4-5 in-game days.

## Fox
Foxes can carry items in their mouths (shows up physically in their mouths). If a fox is holding a totem of undying, it can revive itself if it dies. Similarly, if a fox picks up an enchanted item, it will inherit those enchants. These items are usually found on the ground, with picking up food items preferred. 

Another behavior foxes have are attacking other mobs. They will attack chickens, rabbits, cod, salmon, tropical fish, and turtles. They will also attack wolves if they are attacked by them first. Interestingly, red foxes prefer to attack chickens, rabbits, and baby turtles. Baby foxes typically follow their parent however they are not able to swim. So if the parent goes to a body of water, the baby fox can drown. 

For breeding, use sweet berries or glow berries. Breeding a red fox with a white fox will result in either a red fox or a snow fox with a 50% chance for either. The offspring will trust the player and can be tamed by leading it away from the parent foxes. 
