options( java.parameters = "-Xmx4g" )
getwd()
setwd("C:/ProgramExt/R/")
getwd()

# Using Ms Excel data
library(XLConnect)               # load XLConnect package 
wk = loadWorkbook("DataExecution.xlsx") 
myData = readWorksheet(wk, sheet="DataCombined")

# and attach the data
attach(myData)

# ask for a summary of the data
summary(myData)
cor(AxiomCount, RAM_KB, method="pearson")

# make a plot of RAM_KB vs. AxiomCount with COLORS according to GROUPS.
 plot(AxiomCount, RAM_KB, col=c("orange","blue","green")[Grp], main="SNOMED CT on ELK (Poly.Reg.)")

# now, let's fit a linear regression
model1 <- lm(RAM_KB ~ AxiomCount)
summary(model1)
# and add the line to the plot...make it thick and red...
abline(model1, lwd=3, col="orange")


# try fitting a model that includes AxiomCount ^2 as well
model2 <- lm(RAM_KB ~ poly(AxiomCount, degree=2, raw=T))
summary(model2)
# now, let's add this model to the plot, using a thick green line
lines(smooth.spline(AxiomCount, predict(model2)), col="green", lwd=3)
# let's test whether this model can work for 4th scenario (Gluten - Brain structure)
test2_x<-data.frame(AxiomCount=c(623676, 47, 2, 42, 623676, 47))
test2<-predict(model2,test2_x)
test2


# try fitting a model that includes AxiomCount^3 as well
model3 <- lm(RAM_KB ~ poly(AxiomCount, degree=3, raw=T))
summary(model3)
# now, let's add this model to the plot, using a thick red line
lines(smooth.spline(AxiomCount, predict(model3)), col="blue", lwd=3)

# let's test whether this model can work for 4th scenario (Gluten - Brain structure)
test3_x<-data.frame(AxiomCount=c(623676, 47, 2, 42, 623676, 47))
test3<-predict(model3,test3_x)
test3

#          1          2          3          4          5          6 
# 1118033.54   24298.62   24172.99   24284.67 1118033.54   24298.62 
