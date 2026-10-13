%number of days in the world
numberofdays = 200000;

%number of people
people = 100;

%starting money for each person
startmoney = 100;

%a record of standard deviation throughout the run
deviation = zeros(1, numberofdays);

%Matrix of everyone's money over time
peoplemoneyovertime = zeros(people, numberofdays);


h = waitbar(0, 'money changing hands...');


%hold on;
%money - random
c = ceil(100.*rand(1,people));
c = c + 50;

for days = 1: numberofdays;

    
    
    %cycle through our 20 agents
    for n = 1:people;

            r = ceil(people.*rand(1,1));
            
    %if n == r (i.e. if I'm me) don't do anything
    %also do nothing if I have no money to give
   
    
        if (n~=r && c(1,n)>=0);
   
            

          %  disp('r before add = ')
          %  disp(c(1,r));
          %  disp('n before minus = ')
          %  disp(c(1,n));

          
          
            c(1,r) = c (1,r) + 1;
            c(1,n) = c (1,n) - 1;

           % disp('r after add = ')
           % disp(c(1,r));
           % disp('n after minus = ')
           % disp(c(1,n));

        %else disp('r and n were equal');
        
        end;

    %record the current money value for n person
    peoplemoneyovertime(n, days) = c(1,n);
        
        
    end;
       
    deviation(1,days) = std(c);
    
    waitbar(days/numberofdays);
    
end;

subplot(221), plot(c);

subplot(222), plot(deviation);

axisv = 1:days;

subplot(223), plot(axisv, peoplemoneyovertime);

subplot(224), hist(c);



disp('mean:');
disp(mean(c));

disp('standard deviation:');
disp(std(c));

close(h);




